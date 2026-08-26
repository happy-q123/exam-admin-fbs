package com.ai.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.util.List;

public record WrongQuestionAssistantResponse(
        @JsonSerialize(using = ToStringSerializer.class)
        Long conversationId,
        @JsonSerialize(using = ToStringSerializer.class)
        Long questionId,
        String answer,
        int attempts,
        boolean qualityPassed,
        String qualityNote,
        List<String> citations,
        String agentRunId
) {
}
