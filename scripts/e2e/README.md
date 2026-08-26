# 生产同构 E2E 门禁

这些脚本只做探测和测试，不会重置数据库、删除容器或修改仓库外的配置。运行前请在当前 PowerShell 会话中提供测试环境凭据：

```powershell
$env:E2E_DB_PASSWORD = "<test database password>"
$env:E2E_REDIS_PASSWORD = "<test redis password>"
$env:E2E_ACCESS_TOKEN = "<short-lived test JWT>"
$env:MAVEN_CMD = "<path to mvn.cmd>"
```

执行健康检查和后端门禁：

```powershell
.\scripts\e2e\health-check.ps1
.\scripts\e2e\backend-gate.ps1

# 真实模型评测（需要一个隔离测试账号和题目 ID）
$env:E2E_QUESTION_ID = "<test question id>"
.\scripts\e2e\ai-evaluation.ps1
```

## 隔离约定

- 端到端账号使用 `e2e_<scenario>_<timestamp>` 前缀；考试标题、知识库 `documentKey`、会话查询也使用相同场景前缀。
- 测试数据只能落在测试账号创建的考试、题目、知识库文档和 AI 会话中，不使用固定业务账号或固定主键作为断言依据。
- 清理时遵循外键顺序：AI 反馈/步骤 → AI 运行记录/消息关系 → AI 会话/知识库切片与向量 → 答题/监考事件 → 报名/考试题目关系 → 考试/题目 → 测试账号。默认不执行清理，先根据前缀查询并人工确认。
- `UserMapperTest`、`QuestionOptionTest` 等旧测试会写入固定 ID 或固定业务数据，不属于 CI 门禁；需要单独在隔离数据库运行，禁止在共享开发库直接执行。

## 门禁范围

后端门禁覆盖 AI 答案质量与异常协议、考试模块数据库集成查询、RocketMQ 生产者连接。`ai-evaluation.ps1` 进一步通过真实网关和模型验证五段式答案、多轮会话、重试上限与提示注入兜底。浏览器回归还需要使用 Playwright 完成学生/教师考试流程、错题助手多轮 RAG、STOMP、监考 WebSocket 和双浏览器 WebRTC；这些依赖登录态和真实运行中的前端/网关，不能用 Maven 单测替代。
