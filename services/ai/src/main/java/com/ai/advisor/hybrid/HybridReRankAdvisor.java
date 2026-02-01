package com.ai.advisor.hybrid;

import com.ai.dto.Result;
import com.ai.service.agent.ZhiPuRerankService;
import com.ai.service.common.AiChatComposeService;
import com.ai.utils.LocalRagMetaDataUtil;
import com.domain.entity.LocalRag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * description rag搜索，支持redis缓存+数据库查询和搜索
 * author zzq
 * date 2026/2/1 17:42
*/

@Slf4j
public class HybridReRankAdvisor implements BaseAdvisor {
    private final int order;
    private final VectorStore ragVectorStore;
    private final ZhiPuRerankService rerankService;
    private final AiChatComposeService aiChatComposeService;

    // 基础搜索配置（TopK等），但Query和Filter会在运行时动态构建
    private SearchRequest baseSearchRequest;

    private PromptTemplate rerankPromptTemplate = PromptTemplate.builder()
            .template("下面是排好序的上下文以及对应的得分，请根据用户问题和上下文来回答。\n上下文列表：\n{context}")
            .build();

    // --- 1. 构造方法重构：全参构造 ---
    public HybridReRankAdvisor(ZhiPuRerankService rerankService, VectorStore ragVectorStore, int order,
                               AiChatComposeService aiChatComposeService, SearchRequest baseSearchRequest,
                               PromptTemplate rerankPromptTemplate) {
        this.order = order;
        this.rerankService = rerankService;
        this.ragVectorStore = ragVectorStore;
        this.aiChatComposeService = aiChatComposeService;
        // 如果外部未传入 SearchRequest，给一个默认的基础配置（仅包含topK等，不含Query）
        this.baseSearchRequest = baseSearchRequest != null ? baseSearchRequest : SearchRequest.builder().topK(5).build();
        if (rerankPromptTemplate != null) {
            this.rerankPromptTemplate = rerankPromptTemplate;
        }
    }

    // --- 构造方法重载，便于不同场景调用 ---
    public HybridReRankAdvisor(ZhiPuRerankService rerankService, VectorStore ragVectorStore, int order,
                               AiChatComposeService aiChatComposeService) {
        this(rerankService, ragVectorStore, order, aiChatComposeService, null, null);
    }

    public HybridReRankAdvisor(ZhiPuRerankService rerankService, VectorStore ragVectorStore, int order,
                               AiChatComposeService aiChatComposeService, SearchRequest searchRequest) {
        this(rerankService, ragVectorStore, order, aiChatComposeService, searchRequest, null);
    }

    public HybridReRankAdvisor(ZhiPuRerankService rerankService, VectorStore ragVectorStore, int order,
                               AiChatComposeService aiChatComposeService, PromptTemplate promptTemplate) {
        this(rerankService, ragVectorStore, order, aiChatComposeService, null, promptTemplate);
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        String query = chatClientRequest.prompt().getUserMessage().getText();
        if (query == null || query.isBlank()) {
            return chatClientRequest;
        }

        // 获取上下文中的 ragName
        Object ragNameObj = chatClientRequest.context().get("ragName");

        //删除默认加载逻辑：如果 context 没给 ragName，则完全不进行 RAG
        if (ragNameObj == null) {
            return chatClientRequest;
        }

        // 范化 ragName 为 List<String>，便于后续处理
        List<String> targetRagNames = normalizeRagNames(ragNameObj);
        if (targetRagNames.isEmpty()) {
            return chatClientRequest;
        }

        // 构建包含 Query 和 Filter 的完整 SearchRequest
        SearchRequest finalRequest = buildDynamicSearchRequest(query, targetRagNames);

        List<Document> documents;
        try {
            // Redis 搜索
            log.info("正在从 Redis 搜索内容: {}, 来源范围: {}", query, targetRagNames);
            documents = ragVectorStore.similaritySearch(finalRequest);

            // Redis 未命中，降级查库
            if (documents == null || documents.isEmpty()) {
                log.info("Redis 未命中，降级查询数据库...");
                documents = searchAndCacheFromDatabase(query, targetRagNames, finalRequest.getTopK());
            }

        } catch (Exception e) {
            log.error("RAG 检索异常 (Redis/DB)，跳过增强: {}", e.getMessage());
            return chatClientRequest;
        }

        if (documents == null || documents.isEmpty()) {
            log.warn("未找到相关知识库内容");
            return chatClientRequest;
        }

        // Rerank 与上下文注入
        List<Result> rerankedContext = rerank(query, documents);
        chatClientRequest.context().put("reranked-context", rerankedContext);
        String contextStr = composeContext(rerankedContext);

        String finalUserMessage = "用户问题：" + query + "\n\n" + contextStr;

        return chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt().augmentUserMessage(finalUserMessage))
                .build();
    }

    /**
     * 辅助方法：将 Object (String 或 List) 转为 List<String>
     */
    @SuppressWarnings("unchecked")
    private List<String> normalizeRagNames(Object ragNameObj) {
        if (ragNameObj instanceof String str) {
            return Collections.singletonList(str);
        } else if (ragNameObj instanceof List<?> list) {
            // 简单转义，假设 List 里面都是 String
            return (List<String>) list;
        }
        return Collections.emptyList();
    }

    /**
     * (2) 适配 ragName 为列表或字符串的过滤条件
     */
    private SearchRequest buildDynamicSearchRequest(String query, List<String> ragNames) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        Filter.Expression filter;

        // 根据元数据 key "ragSource" 进行过滤
        if (ragNames.size() == 1) {
            filter = b.eq("ragSource", ragNames.get(0)).build();
        } else {
            filter = b.in("ragSource", ragNames).build();
        }

        return SearchRequest.from(this.baseSearchRequest)
                .query(query)
                .filterExpression(filter)
                .build();
    }

    private List<Document> searchAndCacheFromDatabase(String query, List<String> ragNames, int limit) {
        List<LocalRag> localRags = aiChatComposeService.searchSimilarLocalRag(query, ragNames, limit);

        if (localRags == null || localRags.isEmpty()) {
            return List.of();
        }

        List<Document> documents = LocalRagMetaDataUtil.toDocuments(localRags);

        try {
            if (!documents.isEmpty()) {
                ragVectorStore.add(documents);
                log.info("数据库结果已回写 Redis, 条数: {}", documents.size());
            }
        } catch (Exception e) {
            log.warn("回写 Redis 失败: {}", e.getMessage());
        }
        return documents;
    }

    public String composeContext(List<Result> results) {
        String contextString = results.stream()
                .map(r -> String.format("[得分: %.4f\n内容: %s]\n---", r.score(), r.document()))
                .collect(Collectors.joining("\n"));
        return this.rerankPromptTemplate.render(Map.of("context", contextString));
    }

    public List<Result> rerank(String query, List<Document> documents) {
        List<String> texts = documents.stream().map(Document::getText).toList();
        return rerankService.rerankAndResult(query, texts, documents.size());
    }

    @Override
    public ChatClientResponse after(ChatClientResponse res, AdvisorChain chain) {
        return res;
    }

    @Override
    public int getOrder() {
        return order;
    }

    public static class Builder {
        private ZhiPuRerankService rerankService;
        private VectorStore vectorStore;
        private int order;
        private PromptTemplate rerankPromptTemplate;
        private SearchRequest searchRequest;
        private AiChatComposeService aiChatComposeService;

        public Builder rerankService(ZhiPuRerankService s) { this.rerankService = s; return this; }
        public Builder vectorStore(VectorStore v) { this.vectorStore = v; return this; }
        public Builder order(int o) { this.order = o; return this; }
        public Builder aiChatComposeService(AiChatComposeService s) { this.aiChatComposeService = s; return this; }
        public Builder searchRequest(SearchRequest s) { this.searchRequest = s; return this; }
        public Builder rerankPromptTemplate(PromptTemplate t) { this.rerankPromptTemplate = t; return this; }

        public HybridReRankAdvisor build() {
            if (rerankService == null || vectorStore == null || aiChatComposeService == null) {
                throw new IllegalArgumentException("RerankService, VectorStore, AiChatComposeService 均为必填项");
            }
            return new HybridReRankAdvisor(rerankService, vectorStore, order, aiChatComposeService, searchRequest, rerankPromptTemplate);
        }
    }
}