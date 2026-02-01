package com.ai.advisor.hybrid;

import com.ai.service.common.AiChatComposeService;
import com.ai.utils.ChatMessageMetaDataUtil;
import com.domain.dto.ChatMessageComposeDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * description 历史消息搜索，支持redis缓存+数据库查询和搜索
 * author zzq
 * date 2026/2/1 17:42
*/

@Slf4j
public class HybridHistorySearchAdvisor implements BaseAdvisor {

    private static final int DEFAULT_TOP_K = 10;
    private static final String CONTEXT_KEY_USER_TEXT = "hybrid_memory_user_text";
    private static final String CONTEXT_KEY_REQUEST_TIMESTAMP = "request_timestamp";

    private static final PromptTemplate DEFAULT_SYSTEM_PROMPT_TEMPLATE = new PromptTemplate(
            "{instructions}\n\n" +
                    "You have access to the conversation history (LONG_TERM_MEMORY).\n" +
                    "Use this history to provide context-aware responses.\n" +
                    "---------------------\n" +
                    "LONG_TERM_MEMORY:\n{long_term_memory}\n" +
                    "---------------------\n");

    private final VectorStore vectorStore;
    private final AiChatComposeService aiChatComposeService;
    private final PromptTemplate systemPromptTemplate;
    private final int defaultTopK;
    private final int order;
    private final Scheduler scheduler;

    // 引入 Mapper
    private final ChatMessageMetaDataUtil chatMessageMetaDataUtil = new ChatMessageMetaDataUtil();

    private HybridHistorySearchAdvisor(Builder builder) {
        this.vectorStore = builder.vectorStore;
        this.aiChatComposeService = builder.aiChatComposeService;
        this.systemPromptTemplate = builder.systemPromptTemplate;
        this.defaultTopK = builder.defaultTopK;
        this.order = builder.order;
        this.scheduler = builder.scheduler;
    }

    public static Builder builder(VectorStore vectorStore, AiChatComposeService aiChatComposeService) {
        return new Builder(vectorStore, aiChatComposeService);
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        request.context().put(CONTEXT_KEY_REQUEST_TIMESTAMP, LocalDateTime.now());
        String query = request.prompt().getUserMessage().getText();

        if (query == null || query.isBlank()) return request;

        request.context().put(CONTEXT_KEY_USER_TEXT, query);

        // 检索逻辑
        List<Document> retrievedDocs = doRetrieval(query, request.context());

        if (!retrievedDocs.isEmpty()) {
            String longTermMemory = retrievedDocs.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining(System.lineSeparator() + "---" + System.lineSeparator()));

            SystemMessage systemMessage = request.prompt().getSystemMessage();
            String existingInstructions = (systemMessage != null) ? systemMessage.getText() : "";

            String augmentedText = this.systemPromptTemplate.render(Map.of(
                    "instructions", existingInstructions,
                    "long_term_memory", longTermMemory));

            return request.mutate()
                    .prompt(request.prompt().augmentSystemMessage(augmentedText))
                    .build();
        }
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        return response;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientRequest processedRequest = this.before(request, chain);
        ChatClientResponse response = chain.nextCall(processedRequest);
        // 提取保存逻辑，代码更清晰
        saveContextToMemory(request, response);
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        Flux<ChatClientResponse> responseStream = Mono.just(request)
                .publishOn(this.scheduler)
                .map(req -> this.before(req, chain))
                .flatMapMany(chain::nextStream);

        return new ChatClientMessageAggregator()
                .aggregateChatClientResponse(responseStream, completedResponse -> {
                    saveContextToMemory(request, completedResponse);
                });
    }

    // --- 内部逻辑 (现在的职责是协调，而不是处理细节) ---

    private void saveContextToMemory(ChatClientRequest request, ChatClientResponse response) {
        String userText = (String) request.context().get(CONTEXT_KEY_USER_TEXT);
        this.saveToMemory(userText, response, request.context());
    }

    private List<Document> doRetrieval(String query, Map<String, Object> context) {
        List<Document> retrievedDocs = new ArrayList<>();
        int topK = getChatMemoryTopK(context);

        String userId = String.valueOf(context.getOrDefault("userId", ""));
        String conversationId = String.valueOf(context.getOrDefault("conversationId", ""));

        // L1: Redis
        boolean hitRedis = false;
        if (!userId.isBlank() && !conversationId.isBlank()) {
            log.info("Try Redis search");
            try {
                // 使用 mapper 构建过滤表达式
                String filter = chatMessageMetaDataUtil.buildFilterExpression(userId, conversationId);

                SearchRequest searchRequest = SearchRequest.builder()
                        .query(query).topK(topK).filterExpression(filter).build();

                retrievedDocs = this.vectorStore.similaritySearch(searchRequest);
                if (!retrievedDocs.isEmpty())
                    hitRedis = true;
            } catch (Exception e) {
                log.warn("Redis search failed: {}", e.getMessage());
            }
        }

        // L2: DB Fallback
        if (!hitRedis) {
            log.info("Try DB search");
            try {
                Long uid = parseLongSafely(userId);
                Long cid = parseLongSafely(conversationId);
                if (uid != null && cid != null) {
                    List<ChatMessageComposeDto> dbResults = aiChatComposeService.searchSimilarMessages(uid, cid, query, topK);
                    if (dbResults != null) {
                        retrievedDocs = dbResults.stream().map(chatMessageMetaDataUtil::fromDbEntity).toList();
                    }
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

        Object startTimeObj = context.get(CONTEXT_KEY_REQUEST_TIMESTAMP);
        LocalDateTime userSendTime = (startTimeObj instanceof LocalDateTime) ? (LocalDateTime) startTimeObj : LocalDateTime.now();
        LocalDateTime aiResponseTime = LocalDateTime.now();

        String userIdStr = String.valueOf(context.getOrDefault("userId", "0"));
        String conversationIdStr = String.valueOf(context.getOrDefault("conversationId", "0"));

        try {
            Long conversationId = parseLongSafely(conversationIdStr);
            // 1. Save DB
            if (conversationId != null) {
                aiChatComposeService.createMessage(conversationId, userText, userSendTime, aiContent, aiResponseTime);
            }

            // 2. Save Redis (使用 Mapper 构建 Document)
            Document doc = chatMessageMetaDataUtil.toRedisDocument(
                    userText, aiContent, userSendTime, aiResponseTime, userIdStr, conversationIdStr
            );

            this.vectorStore.add(List.of(doc));

        } catch (Exception e) {
            log.error("Save memory failed", e);
        }
    }

    private int getChatMemoryTopK(Map<String, Object> context) {
        Object val = context.get("chat_memory_top_k");
        return val != null ? Integer.parseInt(val.toString()) : this.defaultTopK;
    }

    private Long parseLongSafely(String val) {
        try {
            return Long.valueOf(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // --- Boilerplate (Order, Scheduler, Builder) ---
    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public Scheduler getScheduler() {
        return this.scheduler;
    }

    public static final class Builder {
        private final VectorStore vectorStore;
        private final AiChatComposeService aiChatComposeService;
        private PromptTemplate systemPromptTemplate = HybridHistorySearchAdvisor.DEFAULT_SYSTEM_PROMPT_TEMPLATE;
        private int defaultTopK = 10;
        private Scheduler scheduler = Schedulers.boundedElastic();
        private int order = 0;

        public Builder(VectorStore vectorStore, AiChatComposeService aiChatComposeService) {
            this.aiChatComposeService = aiChatComposeService;
            Assert.notNull(vectorStore, "VectorStore cannot be null");
            Assert.notNull(aiChatComposeService, "AiChatComposeService cannot be null");
            this.vectorStore = vectorStore;
        }

        public Builder systemPromptTemplate(PromptTemplate t) {
            this.systemPromptTemplate = t;
            return this;
        }

        public Builder defaultTopK(int k) {
            this.defaultTopK = k;
            return this;
        }

        public Builder scheduler(Scheduler s) {
            this.scheduler = s;
            return this;
        }

        public Builder order(int o) {
            this.order = o;
            return this;
        }

        public HybridHistorySearchAdvisor build() {
            return new HybridHistorySearchAdvisor(this);
        }
    }
}