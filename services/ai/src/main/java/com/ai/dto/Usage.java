package com.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
        @JsonProperty("total_tokens") Integer totalTokens,
        @JsonProperty("prompt_tokens") Integer promptTokens,
        @JsonProperty("completion_tokens") Integer completionTokens
    ) {}