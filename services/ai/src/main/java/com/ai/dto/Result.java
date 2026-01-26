package com.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Result(
        @JsonProperty("index") Integer index,           // 原文档在列表中的索引
        @JsonProperty("relevance_score") Double score,  // 相关性分数
        @JsonProperty("document") String document       // (可选) API可能返回的文档片段
) {}
