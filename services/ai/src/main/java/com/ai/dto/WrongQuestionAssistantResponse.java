package com.ai.dto;

import java.util.List;

public record WrongQuestionAssistantResponse(
        Long conversationId,
        Long questionId,
        String answer,
        int attempts,
        boolean qualityPassed,
        String qualityNote,
        List<String> citations,
        String agentRunId
) {
}
