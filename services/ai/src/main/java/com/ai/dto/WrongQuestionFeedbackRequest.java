package com.ai.dto;

public record WrongQuestionFeedbackRequest(String agentRunId, String rating, String reason) {
}
