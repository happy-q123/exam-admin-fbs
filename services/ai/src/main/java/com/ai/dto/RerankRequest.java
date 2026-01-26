package com.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RerankRequest(
    @JsonProperty("model") String model,      // 模型名称，如 "rerank"
    @JsonProperty("query") String query,      // 用户的问题
    @JsonProperty("documents") List<String> documents, // 待排序的文档列表
    @JsonProperty("top_n") Integer topN,       // 返回前 N 个结果
    @JsonProperty("return_documents") boolean is_return_documents,
    @JsonProperty("return_raw_scores") boolean is_return_raw_scores
) {
    //隐藏了一个全参数的构造函数，下面两个构造函数中一个依赖全参构造，一个依赖刚刚那个
    //通过下面两个构造函数的任何一个，就定死model为rerank，以及返回原文本

    public RerankRequest(String query, List<String> documents) {
        this("rerank",query, documents, 5,true,true);
    }

    public RerankRequest(String query, List<String> documents, Integer topN) {
        this( "rerank",query, documents, topN,true,true);
    }
}