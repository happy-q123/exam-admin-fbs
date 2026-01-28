这两个文件组合在一起，相当于为你的在线考试系统搭建了一个**“带安检的高速通信大楼”**。

下面我用通俗的语言，配合你的业务场景（在线监考），来解释这两段代码到底干了什么：

### 1. `WebSocketConfig.java`：大楼的基础设施

这个类负责定义大楼的**入口、通道和广播系统**。

* **`registerStompEndpoints` (定义大门)**
* **做了什么**：开放了 `/ws` 这个端口。
* **效果**：前端（Vue）必须连接 `ws://localhost:8080/ws` 才能进来。同时开启了 `setAllowedOriginPatterns("*")`，允许跨域，这样你的前端不管部署在哪里（比如 8081 端口）都能连上。
* **SockJS**：开启了 SockJS 支持，如果考生的浏览器太老不支持 WebSocket，会自动降级为 HTTP 轮询，保证连接不断。


* **`configureMessageBroker` (定义传达室和暗号)**
* **`/app` 前缀**：这是**“寄信给服务器”**的专用通道。比如考生要发视频流信令，前端发给 `/app/attemptVideo`，Spring 就会自动把信送到你 Controller 里的 `@MessageMapping("/attemptVideo")` 方法。
* **`/topic` 和 `/queue` (或 `/user`)**：这是**“服务器广播”**的通道。
* `/topic` 是大喇叭，发给这里消息，所有订阅的人都能听到（比如“考试开始”通知）。
* `/user` 是私人信箱，发给这里，只有指定的人能收到（比如“警告考生张三作弊”）。


* **心跳机制 (`setHeartbeatValue`)**：
* **做了什么**：设置了 `10000, 10000`（10秒）。
* **效果**：服务器每 10 秒会问前端：“你还在吗？”，前端也要每 10 秒回一句“我还在”。如果网线拔了，服务器几十秒内没收到回复，就会自动断开连接，把该考生标记为“离线”。




* **`configureClientInboundChannel` (安设安检口)**
* **做了什么**：把我们写的 `WebSocketChannelInterceptor` 注册进去了。
* **效果**：所有消息进来之前，都要先经过这个拦截器。



---

### 2. `WebSocketChannelInterceptor.java`：大楼的保安

这个类是系统的**门神**，负责在连接建立的第一时间查验身份。

* **拦截 `CONNECT` 指令**
* **做了什么**：WebSocket 建立连接后，前端会发第一条 STOMP 协议消息叫 `CONNECT`。拦截器只盯着这一条看，后续的普通聊天消息它不管（提高性能）。


* **查验 Token (`jwtDecoder`)**
* **做了什么**：它从头部的 `Authorization` 里拿出 Token，扔给 Spring Security 的 `JwtDecoder` 去验真伪。
* **效果**：
* 如果 Token 是伪造的/过期的 -> **直接报错**，连接断开，坏人进不来。
* 如果 Token 是真的 -> 解析出里面的信息（比如 `sub` 是用户ID，`scope` 是权限）。




* **绑定身份 (`accessor.setUser`) —— 最重要的一步！**
* **做了什么**：它把解析出来的用户信息（`JwtAuthenticationToken`）强力胶水一样**粘**在这个 WebSocket Session 上。
* **效果**：
* **彻底解决了“我是谁”的问题**。
* 以后不管这个连接发什么消息，Spring 都知道“这是张三发的”。
* **Controller 里的 `Principal` 不再是 null 了**。你可以直接 `Principal.getName()` 拿到考生 ID，完全不需要前端在 JSON 参数里再传一次 `studentId`（那种方式很不安全，容易被篡改）。





---

### 总结：现在的系统能力

经过这两步配置，你的 WebSocket 系统已经具备了以下能力：

1. **安全**：没有 Token 连不上，Token 过期连不上。
2. **健壮**：自带心跳，考生断网能迅速感知。
3. **智能**：Controller 不需要再查数据库去验证用户，直接从 Session 里拿 ID。
4. **清晰**：前端发给 `/app`，收广播听 `/topic`，收私信听 `/user`，井井有条。

### 3消息代理的详解
这个配置看起来确实有点抽象，特别是各种前缀满天飞。别担心，我用一个生活中的**“邮局”**或**“公司前台”**的例子来给你彻底讲清楚。

**`configureMessageBroker` 的核心作用就是：设置“交通规则”和“分拣中心”。**

你可以把你的 WebSocket 服务器想象成一家**大公司**，这个方法就是在规定前台（Broker）如何处理收到的信件。

---

### 一、 核心概念图解

我们把代码里的配置拆解成三个部分：

1. **`/app`** (应用前缀) = **寄给“办事窗口”的信**
* **方向**：客户端 -> 服务器（Controller）
* **作用**：客户端要**请求**服务器干活（比如：监考老师说“我要看视频”）。


2. **`/topic`, `/queue**` (代理前缀) = **寄给“公告栏”或“信箱”的信**
* **方向**：服务器（或客户端） -> 客户端
* **作用**：服务器干完活了，把结果**推送**给客户端（比如：服务器把视频数据推给学生）。


3. **`/user`** (用户前缀) = **寄给“私人秘书”的信**
* **作用**：处理点对点的隐私消息。



---

### 二、 详细代码拆解

#### 1. `registry.setApplicationDestinationPrefixes("/app");`

> **通俗解释：这是“办事处”的专用通道。**

* **场景**：
  前端（考生）想告诉服务器：“我要交卷了”。
* **流程**：
1. 前端发送消息到：`/app/submitPaper`。
2. **Broker（前台）** 看到前缀是 `/app`，它心里想：“哦，这是要找办事员干活的。”
3. Broker **不直接转发**，而是把消息剥掉 `/app`，变成 `/submitPaper`。
4. Broker 把它交给你的 Java 代码中写了 `@MessageMapping("/submitPaper")` 的那个 Controller 方法去执行。



**总结**：凡是前端发给 `/app` 开头的消息，都会进入你的 Java **Controller 方法**里。

---

#### 2. `registry.enableSimpleBroker("/topic", "/queue");`

> **通俗解释：这是“广播站”和“私人信箱”的通道。**

这里的 `SimpleBroker` 是 Spring 内置的一个**内存消息代理**。它不负责处理业务逻辑，它只负责**转发**。

* **场景 A（广播 /topic）**：
  服务器要通知所有人：“考试结束了”。
1. Controller 算出结果，通过 `messagingTemplate` 发送到 `/topic/exam/end`。
2. **Broker** 看到前缀是 `/topic`，它心里想：“这是个订阅消息，不需要 Controller 处理，直接发给订阅者。”
3. Broker 查找所有订阅了 `/topic/exam/end` 的前端客户端，把消息推给他们。


* **场景 B（点对点 /queue）**：
  服务器要通知张三：“你作弊了”。
1. 通常配合 `/user` 使用（下面讲）。但逻辑也是：这是发给客户端看的，不是发给服务器计算的。



**总结**：凡是发给 `/topic` 或 `/queue` 的消息，Spring 会直接把它**原封不动**地甩给订阅了该地址的**前端**，而不会进入 Controller。

---

#### 3. `registry.setUserDestinationPrefix("/user");`

> **通俗解释：这是“实名投递”的魔法。**

这是最难理解、但最强大的部分。它解决了**“我怎么只发给张三，不发给李四？”**的问题。

* **问题**：
  如果只有 `/topic`，你发消息到 `/topic/message`，所有订阅这个地址的人都收到了。
  如果你想发给张三，你可能会想造一个地址 `/topic/zhangsan`。但这样不安全，李四如果猜到了订阅这个地址，也能收到。
* **魔法流程**：
1. **前端（张三）** 订阅：`/user/queue/errors` (注意：他订阅的是通用的 `/user` 开头)。
2. **Broker** 内部转换：Spring 发现是张三连上来了，它会自动把这个订阅转换成一个**只有张三能收到**的、带有唯一 Session ID 的内部地址，比如 `/queue/errors-zhangsan-session123`。
3. **后端** 发送：你调用 `convertAndSendToUser("zhangsan", "/queue/errors", "消息")`。
4. **Broker** 路由：它自动找到张三的那个唯一 Session ID，把消息精准投递过去。李四就算订阅了 `/user/queue/errors`，收到的也是属于李四的空信箱。



---

### 三、 一张图看懂数据流向

假设我们要实现“监考老师(Teacher) 发起视频请求给 考生(Student)”：

1. **请求阶段 (前端 -> 后端)**
* Teacher 前端发送：`/app/attemptVideo`
* **Broker**：看到 `/app` -> 转给 **Controller** (`@MessageMapping("/attemptVideo")`)。


2. **处理阶段 (Java 逻辑)**
* Controller：收到请求，校验权限，拿到目标 Student 的 ID。


3. **响应阶段 (后端 -> 前端)**
* Controller 调用：`messagingTemplate.convertAndSendToUser("StudentID", "/queue/video-request", ...)`
* **Broker**：
* 看到是 `ToUser` -> 解析出 Student 的真实 Session。
* 看到路径包含 `/queue` -> 这是一个分发动作。


* **最终**：Student 的前端收到了 `/user/queue/video-request` 的消息。



### 四、 你的疑问解答

> `registry.enableSimpleBroker("/topic", "/queue")`

**问**：为什么这里要写两个？
**答**：这只是一个约定（Convention）。

* Spring 并不强制你必须用这两个词。你完全可以写 `enableSimpleBroker("/public", "/private")`。
* 但是，**行业标准**是：
* 用 `/topic` 代表 **一对多**（广播，像订阅报纸）。
* 用 `/queue` 代表 **一对一**（私聊，像排队领饭）。


* **加上这两个配置，意味着：** 如果消息是发往这两个开头的，Broker 就会**接管**并负责**投递**给客户端。

> `te.setPoolSize(1); ... setHeartbeatValue`

**问**：这个心跳是干嘛的？
**答**：

* WebSocket 连接是长连接。如果中间网络设备（如路由器、Nginx）发现一条连接 60 秒没数据传输，可能会认为连接死了，把它掐断。
* **心跳**就是：每隔 10 秒，服务器给前端发一个“ping”，前端回一个“pong”。
* **作用**：1. 保持连接活跃，防止被掐断；2. 快速发现对方是不是断网了（如果发 ping 不回，说明断网了，服务器立马可以把用户标记为离线）。

---

### 总结：你应该怎么用？

在你的代码里，保持目前的配置完全没问题，它是最标准的写法：

1. **想让后端干活**（存库、计算）：前端发给 `/app/xxx`。
2. **想广播给所有人**：后端发给 `/topic/xxx`。
3. **想私发给某个人**：后端用 `convertAndSendToUser` 发给 `/queue/xxx`。

现在理解了吗？它就是一个能够识别不同信封前缀的智能分拣机。