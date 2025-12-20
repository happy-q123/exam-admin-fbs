package com.message.config;

import com.message.interceptor.WebSocketChannelInterceptor;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * description websocket核心配置类。配置握手入口、心跳、拦截器、消息代理
 * author zzq
 * date 2025/12/20 21:32
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Resource
    private WebSocketChannelInterceptor webSocketChannelInterceptor;

    /**
     * 注册 STOMP 端点 (握手地址,前端入口)
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 通道 1：纯原生 WebSocket (给 Apifox, 安卓, iOS, 桌面端用)
        // 连接地址: ws://localhost:8080/ws
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        // 通道 2：SockJS 支持 (给 Web 浏览器用)
        // 连接地址: http://localhost:8080/ws-sockjs
        // 注意：这里必须换个名字，不能也叫 /ws
        registry.addEndpoint("/ws-sockjs")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * 配置输入通道拦截器 (核心：处理 Token 鉴权)
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 添加我们自定义的拦截器，用于在连接建立前解析 Token
        registration.interceptors(webSocketChannelInterceptor);
    }

    /**
     * 配置消息代理 (Message Broker)（前端发过来的消息，由下面配置进行统一调度）
     * /app/**会被路由到 @MessageMapping 注解的方法（controller）
     * /topic/**是订阅消息，不需要 Controller 处理，直接发给订阅者Broker查找所有订阅了/topic/**的前端客户端，把消息推给他们。
     * /queue/**是点对点消息，直接发给指定用户。/queue/**需要搭配/user使用，具体可看“WebsocketConfig说明.md”的第三部分
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 配置心跳调度器（非常重要，防止连接假死）
        ThreadPoolTaskScheduler te = new ThreadPoolTaskScheduler();
        te.setPoolSize(1);
        te.setThreadNamePrefix("ws-heartbeat-thread-");
        te.initialize();

        // 启用内置的 SimpleBroker
        registry.enableSimpleBroker("/topic", "/queue") // 广播用/topic, 点对点用/queue
                .setHeartbeatValue(new long[]{10000, 10000}) // [发心跳间隔, 收心跳间隔] 单位毫秒
                .setTaskScheduler(te);

        // 全局使用的消息前缀（客户端发送消息给服务器时，需带上这个前缀）
        // 例如：客户端发 /app/attemptVideo，会路由到 @MessageMapping("/attemptVideo")
        registry.setApplicationDestinationPrefixes("/app"); // 建议改为 /app，更符合规范，你原来的 /socket 也可以

        // 点对点使用的前缀（默认就是 /user，这里显式配置一下更清晰）
        registry.setUserDestinationPrefix("/user");
    }
}