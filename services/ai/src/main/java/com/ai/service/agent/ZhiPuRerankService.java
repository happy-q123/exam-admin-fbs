package com.ai.service.agent;


import com.ai.dto.RerankRequest;
import com.ai.dto.RerankResponse;
import com.ai.dto.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class ZhiPuRerankService {

    private final RestClient restClient;
    private static final String RERANK_URL = "https://open.bigmodel.cn/api/paas/v4/rerank";

    // 修改点：传入 builder，这样测试时可以塞入 Mock 对象
    public ZhiPuRerankService(RestClient.Builder builder, @Value("${spring.ai.zhipuai.api-key}") String apiKey) {
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
    public List<Result> rerankAndResult(String query, List<String> documents, int topN){
        RerankResponse response = rerank(query, documents, topN);
        return response.results();
    }

    //排序并返回context组成的list，list元素为排序后的顺序
    public List<String> rerankAndContext(String query, List<String> documents, int topN){
        RerankResponse response = rerank(query, documents, topN);
        return response.results().stream().map(Result::document).toList();
    }
}