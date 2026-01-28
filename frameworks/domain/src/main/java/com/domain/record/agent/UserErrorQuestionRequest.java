package com.domain.record.agent;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

// 输入类：将 userId 包装在对象中
public record UserErrorQuestionRequest(
    // 这里的字段名 "userId" 会被生成到 JSON Schema 中，AI 会尝试提取名为 userId 的参数
    @JsonPropertyDescription("用户的唯一标识ID") // 可选，增加描述能提高提取准确率
    Long userId
) {}