package com.ai.service.agent;


import com.ai.dto.RerankRequest;
import com.ai.dto.RerankResponse;
import com.ai.dto.Result;
import com.domain.annotation.CacheGoverned;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.IntStream;

@Service
public class ZhiPuRerankService {

    private final RestClient restClient;
    private static final String RERANK_URL = "https://open.bigmodel.cn/api/paas/v4/rerank";

    // 修改点：传入 builder，这样测试时可以塞入 Mock 对象
    public ZhiPuRerankService(RestClient.Builder builder, @Value("${spring.ai.zhipuai.api-key:}") String apiKey) {
        this.restClient = builder
                .baseUrl(RERANK_URL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 执行重排序
     * @param query 用户查询的问题
     * @param documents 检索到的候选文档列表
     * @param topN 需要保留的前几名
     * @return 排序结果
     */
    public RerankResponse rerank(String query, List<String> documents, int topN) {
        RerankRequest request = new RerankRequest(query, documents, topN);
        
        return restClient.post()
                .body(request)
                .retrieve()
                .body(RerankResponse.class);
    }

    //排序并返回Result结果，Result包含index、score、context
    @CacheGoverned(namespace = "rerank", ttlSeconds = 300)
    public List<Result> rerankAndResult(String query, List<String> documents, int topN){
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        try {
            RerankResponse response = rerank(query, documents, topN);
            if (response != null && response.results() != null && !response.results().isEmpty()) {
                return response.results().stream().map(item -> {
                    int index = item.index() == null ? 0 : item.index();
                    String document = item.document();
                    if ((document == null || document.isBlank()) && index >= 0 && index < documents.size()) {
                        document = documents.get(index);
                    }
                    return new Result(index, item.score() == null ? 0D : item.score(), document);
                }).filter(item -> item.document() != null).toList();
            }
        } catch (Exception ignored) {
            // 重排服务不可用时保留向量检索顺序，不能让错题助手整体失败。
        }
        return IntStream.range(0, Math.min(topN, documents.size()))
                .mapToObj(index -> new Result(index, 0D, documents.get(index)))
                .toList();
    }

    //排序并返回context组成的list，list元素为排序后的顺序
    public List<String> rerankAndContext(String query, List<String> documents, int topN){
        return rerankAndResult(query, documents, topN).stream().map(Result::document).toList();
    }
}
