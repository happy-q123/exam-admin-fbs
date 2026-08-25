package com.ai.dto;

/**
 * 错题助手请求。questionId 可选：为空时使用用户最近一条错题作为上下文。
 */
public record WrongQuestionAssistantRequest(
        Long conversationId,
        Long questionId,
        String query
) {
}
