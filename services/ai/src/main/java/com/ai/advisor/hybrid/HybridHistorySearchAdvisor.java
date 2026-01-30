package com.ai.advisor.hybrid;

import com.ai.service.common.AiChatMessageService;
import com.domain.entity.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 混合分层记忆 Advisor (Hybrid Tiered Memory)
 * <p>
 * 适配 Spring AI BaseAdvisor 接口：
 * 1. Sync 模式：利用 default adviseCall -> before (检索) -> next -> after (保存)
 * 2. Stream 模式：Override adviseStream -> before (检索) -> next -> Aggregator (聚合) -> callback (保存)
 */
@Slf4j
public class HybridHistorySearchAdvisor implements BaseAdvisor {

    // --- 常量配置 ---
    private static final int DEFAULT_TOP_K = 10;
    private static final String CONTEXT_KEY_USER_TEXT = "hybrid_memory_user_text"; // 用于在上下文中传递用户问题
    private static final String SOURCE_FIELD_KEY = "messageSource";
    private static final String CONTEXT_KEY_REQUEST_TIMESTAMP = "request_timestamp";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final PromptTemplate DEFAULT_SYSTEM_PROMPT_TEMPLATE = new PromptTemplate(
            "{instructions}\n\n" +
                    "You have access to the conversation history (LONG_TERM_MEMORY).\n" +
                    "Use this history to provide context-aware responses.\n" +
                    "---------------------\n" +
                    "LONG_TERM_MEMORY:\n{long_term_memory}\n" +
                    "---------------------\n");

    // --- 组件 ---
    private final VectorStore vectorStore;          // L1: Redis
    private final AiChatMessageService chatMessageService; // L2: DB
    private final PromptTemplate systemPromptTemplate;
    private final int defaultTopK;
    private final int order;
    private final Scheduler scheduler; // 用于流式处理的线程调度

    // --- 构造器 (使用 Builder 模式) ---
    private HybridHistorySearchAdvisor(Builder builder) {
        this.vectorStore = builder.vectorStore;
        this.chatMessageService = builder.chatMessageService;
        this.systemPromptTemplate = builder.systemPromptTemplate;
        this.defaultTopK = builder.defaultTopK;
        this.order = builder.order;
        this.scheduler = builder.scheduler;
    }

    public static Builder builder(VectorStore vectorStore, AiChatMessageService aiChatMessageService) {
        return new Builder(vectorStore, aiChatMessageService);
    }

    /**
     * 【RAG 检索阶段】
     * 无论同步还是流式，都会先调用此方法。
     * 作用：
     * 1. 提取用户问题放入 Context（供保存阶段使用）。
     * 2. 执行 Redis/DB 检索。
     * 3. 注入 System Prompt。
     */
    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        // 0. 记录用户发送请求的时间 (Start Time)
        request.context().put(CONTEXT_KEY_REQUEST_TIMESTAMP, LocalDateTime.now());

        String query = request.prompt().getUserMessage().getText();

        // 1. 保护性检查
        if (query == null || query.isBlank()) {
            return request;
        }

        // 2. 将 User Query 放入 Context，以便后续保存时读取 (因为 Response 对象里没有 Request 的信息)
        request.context().put(CONTEXT_KEY_USER_TEXT, query);

        // 3. 执行混合检索 (Redis -> DB)
        List<Document> retrievedDocs = doRetrieval(query, request.context());

        // 4. 如果有历史记录，注入 Prompt
        if (!retrievedDocs.isEmpty()) {
            String longTermMemory = retrievedDocs.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining(System.lineSeparator() + "---" + System.lineSeparator()));

            SystemMessage systemMessage = request.prompt().getSystemMessage();
            String existingInstructions = (systemMessage != null) ? systemMessage.getText() : "";

            String augmentedText = this.systemPromptTemplate.render(Map.of(
                    "instructions", existingInstructions,
                    "long_term_memory", longTermMemory));

            // 返回增强后的 Request
            return request.mutate()
                    .prompt(request.prompt().augmentSystemMessage(augmentedText))
                    .build();
        }

        return request;
    }

    /**
     * 【Sync 模式保存阶段】
     * BaseAdvisor.adviseCall 会在 LLM 返回完整响应后自动调用此方法。
     */
    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        // 从 Context 获取之前存入的 User Text (注意：这里需要从 advisorChain 还是 response 获取取决于上下文传递，
        // 但通常 Response 不带 Request 的 Context。在 Sync 模式下，我们通常无法简单拿到 request context。
        // 但 Spring AI 的 Chain 机制通常是闭环的。如果拿不到，Sync 保存可能受限。
        // 最佳实践：Sync 模式下，adviseCall 内部持有 request 引用，但在 after 接口里拿不到 request。
        // *修正策略*：BaseAdvisor 接口限制了 after 拿不到 Request Context。
        // 因此 Sync 模式的保存逻辑最好不要依赖 after，或者需要 ThreadLocal。
        // 为了安全起见，我们建议 Sync 模式的保存逻辑放在 adviseCall 的 override 中，或者在此处不做操作，
        // 而是完全依赖重写 adviseCall/adviseStream。

        // 鉴于 BaseAdvisor 的结构，为了能在 Sync 模式下保存，我们需要拿到 Request Context。
        // 这里的 after 主要是给 "修改 Response" 用的，做副作用(Side Effect)保存不太方便。
        // 但为了实现接口，我们保留空实现或尝试处理。

        // **注意**：此处我们不做保存，因为无法拿到 User Query。
        // Sync 保存逻辑建议 Override adviseCall (见下文) 或者由调用方处理。
        return response;
    }

    /**
     * 【重写 Sync 调用链路】
     * 为了在 Sync 模式下也能保存记忆（需要同时访问 Request 和 Response），我们覆盖默认实现。
     */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 1. 执行 before (检索)
        ChatClientRequest processedRequest = this.before(request, chain);

        // 2. 执行调用
        ChatClientResponse response = chain.nextCall(processedRequest);

        // 3. 执行保存 (这里我们既有 Request 又有 Response)
        String userText = (String) request.context().get(CONTEXT_KEY_USER_TEXT);
        this.saveToMemory(userText, response, request.context());

        return response; // 这里的 response 也可以经过 this.after 处理，如果需要的话
    }

    /**
     * 【重写 Stream 调用链路】
     * 必须重写！因为默认实现的 after 只会收到最后一个 Chunk，无法获得完整对话内容。
     */
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        // 1. 异步执行 before (检索)
        // 使用 Mono.just + map 确保检索在流启动前完成，并利用 publishOn 切换线程
        Flux<ChatClientResponse> responseStream = Mono.just(request)
                .publishOn(this.scheduler)
                .map(req -> this.before(req, chain)) // 执行 RAG
                .flatMapMany(chain::nextStream);     // 启动 LLM 流

        // 2. 使用 Aggregator 监听流
        // Aggregator 会透传流给前端，同时在内部拼接完整文本，当流结束时触发 Consumer
        return new ChatClientMessageAggregator()
                .aggregateChatClientResponse(responseStream, completedResponse -> {
                    log.debug("📝 Stream 完成，触发异步记忆保存...");
                    String userText = (String) request.context().get(CONTEXT_KEY_USER_TEXT);
                    this.saveToMemory(userText, completedResponse, request.context());
                });
    }

    // --- 内部逻辑方法 ---

    private List<Document> doRetrieval(String query, Map<String, Object> context) {
        List<Document> retrievedDocs = new ArrayList<>();
        int topK = getChatMemoryTopK(context);
        boolean hitRedis = false;

        // L1: Redis (示例代码，需根据实际情况放开)
        // try {
        //     String filter = String.format("%s == '%s'", SOURCE_FIELD_KEY, context.getOrDefault(SOURCE_FIELD_KEY, "default"));
        //     SearchRequest searchRequest = SearchRequest.query(query).withTopK(topK).withFilterExpression(filter);
        //     retrievedDocs = this.vectorStore.similaritySearch(searchRequest);
        //     if (!retrievedDocs.isEmpty()) hitRedis = true;
        // } catch (Exception e) { log.warn("Redis search failed: {}", e.getMessage()); }

        // L2: Database Fallback
        if (!hitRedis) {
            try {
                log.info("🔄 降级查询 DB: {}", query);
                List<ChatMessage> dbResults = chatMessageService.searchSimilarMessages(query, topK);
                if (dbResults != null) {
                    retrievedDocs = dbResults.stream().map(this::convertDbEntityToDocument).toList();
                }
            } catch (Exception e) {
                log.error("DB search failed", e);
            }
        }
        return retrievedDocs;
    }

    private void saveToMemory(String userText, ChatClientResponse response, Map<String, Object> context) {
        if (response == null || response.chatResponse() == null || response.chatResponse().getResult() == null) return;

        String aiContent = response.chatResponse().getResult().getOutput().getText();
        if (userText == null || aiContent == null || aiContent.isBlank()) return;

        String sourceValue = (String) context.getOrDefault(SOURCE_FIELD_KEY, "default");

        // --- 时间处理逻辑 ---
        // 1. 获取用户发送时间 (从 Context 中取)
        Object startTimeObj = context.get(CONTEXT_KEY_REQUEST_TIMESTAMP);
        LocalDateTime userSendTime;

        if (startTimeObj instanceof LocalDateTime) {
            userSendTime = (LocalDateTime) startTimeObj;
        } else {
            // 如果某种原因没拿到，降级为当前时间
            userSendTime = LocalDateTime.now();
        }

        // 2. 获取 AI 回复完成时间 (当前时间)
        LocalDateTime aiResponseTime = LocalDateTime.now();

        // 异步/同步写入
        try {
            // Write DB
            chatMessageService.saveChatPair(userText, userSendTime, aiContent, aiResponseTime);

            // Write Redis
            String combined = "User: " + userText + "\nAssistant: " + aiContent;
            Document doc = Document.builder()
                    .text(combined)
                    .metadata(SOURCE_FIELD_KEY, sourceValue)
                    .metadata("type", "conversation_history")
                    .build();
            this.vectorStore.add(List.of(doc));

            log.debug("✅ Memory saved.");
        } catch (Exception e) {
            log.error("❌ Save memory failed", e);
        }
    }

    private Document convertDbEntityToDocument(ChatMessage msg) {
        //格式化时间
        String userTimeStr = msg.getUserCreatedTime()!= null
                ?msg.getUserCreatedTime().format(TIME_FORMATTER)
                :"";

        String aiTimeStr = msg.getAiCreatedTime()!= null
                ?msg.getAiCreatedTime().format(TIME_FORMATTER)
                :"";

        // 2. 拼接带有时间戳的文本
        // 格式示例:
        // [2026-01-30 10:00:00] User问题: 你好
        // [2026-01-30 10:00:05] Assistant回答: 你好！有什么可以帮你的？
        String content = String.format(
                "[%s] User问题: %s\n[%s] Assistant回答: %s",
                userTimeStr,
                msg.getUserContent(),
                aiTimeStr,
                msg.getAiContent()
        );

        return Document.builder()
                .id(msg.getId().toString())
                .text(content) // 注入带时间的文本
                .metadata(SOURCE_FIELD_KEY, "database_fallback")
                // 建议：同时也把时间放入 metadata，方便后续如果有高级检索需求（如：过滤最近一周的对话）
                .metadata("user_timestamp", userTimeStr)
                .metadata("ai_timestamp", aiTimeStr)
                .build();
    }

    private int getChatMemoryTopK(Map<String, Object> context) {
        Object val = context.get("chat_memory_top_k");
        return val != null ? Integer.parseInt(val.toString()) : this.defaultTopK;
    }

    // --- 接口实现 ---

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public Scheduler getScheduler() {
        return this.scheduler;
    }

    // --- Builder ---
    public static final class Builder {
        private final VectorStore vectorStore;
        private final AiChatMessageService chatMessageService;
        private PromptTemplate systemPromptTemplate = HybridHistorySearchAdvisor.DEFAULT_SYSTEM_PROMPT_TEMPLATE;
        private int defaultTopK = 10;
        private Scheduler scheduler = Schedulers.boundedElastic(); // 默认使用弹性线程池
        private int order = 0;

        public Builder(VectorStore vectorStore, AiChatMessageService aiChatMessageService) {
            Assert.notNull(vectorStore, "VectorStore cannot be null");
            Assert.notNull(aiChatMessageService, "AiChatMessageService cannot be null");
            this.vectorStore = vectorStore;
            this.chatMessageService = aiChatMessageService;
        }

        public Builder systemPromptTemplate(PromptTemplate t) { this.systemPromptTemplate = t; return this; }
        public Builder defaultTopK(int k) { this.defaultTopK = k; return this; }
        public Builder scheduler(Scheduler s) { this.scheduler = s; return this; }
        public Builder order(int o) { this.order = o; return this; }

        public HybridHistorySearchAdvisor build() {
            return new HybridHistorySearchAdvisor(this);
        }
    }
}