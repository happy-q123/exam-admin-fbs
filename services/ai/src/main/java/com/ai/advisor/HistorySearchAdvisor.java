package com.ai.advisor;

import io.modelcontextprotocol.util.Assert;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.*;
import java.util.stream.Collectors;

//修改自VectorStoreChatMemoryAdvisor，由于该类无法继承，所以只能复制一份代码
@Slf4j
public class HistorySearchAdvisor implements BaseChatMemoryAdvisor {
    private static final int DEFAULT_TOP_K = 20;
    private static final PromptTemplate DEFAULT_SYSTEM_PROMPT_TEMPLATE = new PromptTemplate(
            "{instructions}\n\nUse the long term conversation memory from the LONG_TERM_MEMORY section to provide accurate answers.\n\n---------------------\nLONG_TERM_MEMORY:\n{long_term_memory}\n---------------------\n");

    // --- 核心组件 ---
    private final VectorStore vectorStore;
    private final PromptTemplate systemPromptTemplate;

    @Getter
    private final Scheduler scheduler;
    private final int defaultTopK;
    private final int order;

    @Getter
    @Setter
    public String messageTypeField = "messageType";

    @Getter
    @Setter
    public String messageSourceFile ="messageSource";

    // 列表 1: 存储要被追加（持久化）的字段名
    private final Set<String> fieldsToPersist = new LinkedHashSet<>();

    // 列表 2: 存储搜索时要被当作 Filter 条件的字段名
    @Getter
    private final Set<String> fieldsToFilter = new LinkedHashSet<>();

    private HistorySearchAdvisor(Builder builder) {
        this.vectorStore = builder.vectorStore;
        this.systemPromptTemplate = builder.systemPromptTemplate;
        this.defaultTopK = builder.defaultTopK;
        this.order = builder.order;
        this.scheduler = builder.scheduler;
        this.fieldsToPersist.addAll(builder.fieldsToPersist);
        this.fieldsToFilter.addAll(builder.fieldsToFilter);
    }

    public static Builder builder(VectorStore vectorStore) {
        return new Builder(vectorStore);
    }

    //添加固定字段
    private void processContext(Map<String, Object> context){
        context.remove("messageType");
        context.put("messageSource", "conversation");
    }
    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        ChatClientRequest processedRequest=request;

        Map<String, Object> context = request.context();
        context.put("messageSource", "conversation");

        // 1. 动态构建 Filter 表达式
        // 完全依赖于 fieldsToFilter 列表和 Context 里的值
        List<String> filterParts = new ArrayList<>();

        for (String fieldKey : fieldsToFilter) {
            // 如果context里相关的值，就作为Filter条件
            if (context.containsKey(fieldKey)) {
                Object value = context.get(fieldKey);
                if (value != null) {
                    filterParts.add(String.format("%s == '%s'", fieldKey, value.toString()));
                }
            }
        }
        String filterExpression = String.join(" && ", filterParts);
        try {
            // 2. 执行搜索
            String query = request.prompt().getUserMessage() != null ? request.prompt().getUserMessage().getText() : "";
            int topK = this.getChatMemoryTopK(context);
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .filterExpression(filterExpression.isEmpty() ? null : filterExpression)
                    .build();

            List<Document> documents = this.vectorStore.similaritySearch(searchRequest);

            // 3. 增强 System Prompt
            String longTermMemory = documents == null ? "" : documents.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining(System.lineSeparator()));

            SystemMessage systemMessage = request.prompt().getSystemMessage();
            String augmentedSystemText = this.systemPromptTemplate.render(Map.of(
                    "instructions", systemMessage.getText(),
                    "long_term_memory", longTermMemory));

            processedRequest = request.mutate()
                    .prompt(request.prompt().augmentSystemMessage(augmentedSystemText))
                    .build();

            // 4. 保存当前用户消息
            UserMessage userMessage = processedRequest.prompt().getUserMessage();
            if (userMessage != null) {
                // 直接传入原始 context，不再需要 tempContext 注入 ID
                this.vectorStore.write(this.toDocuments(List.of(userMessage), context));
            }
        } catch (Exception e) {
            log.error("redis无法使用，将取消使用RAG增强！！！");
        }
        return processedRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        List<Message> assistantMessages = new ArrayList<>();
        if (response.chatResponse() != null) {
            assistantMessages = response.chatResponse().getResults().stream()
                    .map(g -> (Message)g.getOutput())
                    .toList();
        }
        try {
            // 直接传入 Context 保存
            this.vectorStore.write(this.toDocuments(assistantMessages, response.context()));
        }catch (Exception e){
            log.error("redis无法使用！");
        }

        return response;
    }

    /**
     * 将消息转为 Document，并注入 Metadata
     */
    private List<Document> toDocuments(List<Message> messages, Map<String, Object> context) {
        return messages.stream()
                .filter(m -> m.getMessageType() == MessageType.USER || m.getMessageType() == MessageType.ASSISTANT)
                .map(message -> {
                    // 1. 保留 Message 原有的 Metadata
                    Map<String, Object> metadata = new HashMap<>(message.getMetadata() != null ? message.getMetadata() : new HashMap<>());

                    // 2. 遍历 "fieldsToPersist"
                    for (String fieldKey : fieldsToPersist) {
                        if (context.containsKey(fieldKey)) {
                            Object value = context.get(fieldKey);
                            if (value != null) {
                                metadata.put(fieldKey, value);
                            }
                        }
                    }
                    return Document.builder()
                            .text(message.getText())
                            .metadata(metadata)
                            .build();
                })
                .toList();
    }

    // --- 其他 Boilerplate ---
    @Override
    public int getOrder() { return this.order; }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain streamAdvisorChain) {
        return Mono.just(request)
                .publishOn(this.scheduler)
                .map(req -> this.before(req, streamAdvisorChain))
                .flatMapMany(streamAdvisorChain::nextStream)
                .transform(flux -> new org.springframework.ai.chat.client.ChatClientMessageAggregator()
                        .aggregateChatClientResponse(flux, res -> this.after(res, streamAdvisorChain)));
    }

    private int getChatMemoryTopK(Map<String, Object> context) {
        if (context.containsKey("chat_memory_vector_store_top_k")) {
            return Integer.parseInt(context.get("chat_memory_vector_store_top_k").toString());
        }
        return this.defaultTopK;
    }

    // --- Builder ---
    public static final class Builder {
        private final VectorStore vectorStore;
        private PromptTemplate systemPromptTemplate = HistorySearchAdvisor.DEFAULT_SYSTEM_PROMPT_TEMPLATE;
        private int defaultTopK = 20;
        private Scheduler scheduler = BaseAdvisor.DEFAULT_SCHEDULER;
        private int order = 0;

        private Set<String> fieldsToPersist = new LinkedHashSet<>();
        private Set<String> fieldsToFilter = new LinkedHashSet<>();

        public Builder(VectorStore vectorStore) {
            Assert.notNull(vectorStore, "vectorStore cannot be null");
            this.vectorStore = vectorStore;
        }

        // 添加需要持久化的字段
        public Builder persist(String... fields) {
            Collections.addAll(this.fieldsToPersist, fields);
            return this;
        }

        // 添加需要过滤的字段
        public Builder filter(String... fields) {
            Collections.addAll(this.fieldsToFilter, fields);
            return this;
        }

        // 既持久化又过滤 (通常这就包括 conversationId, userId 等)
        public Builder persistAndFilter(String... fields) {
            Collections.addAll(this.fieldsToPersist, fields);
            Collections.addAll(this.fieldsToFilter, fields);
            return this;
        }

        public Builder systemPromptTemplate(PromptTemplate t) { this.systemPromptTemplate = t; return this; }
        public Builder defaultTopK(int k) { this.defaultTopK = k; return this; }
        public Builder scheduler(Scheduler s) { this.scheduler = s; return this; }
        public Builder order(int o) { this.order = o; return this; }

        public HistorySearchAdvisor build() {
            return new HistorySearchAdvisor(this);
        }
    }
}
