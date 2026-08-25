package com.ai.dto;

public record KnowledgeDocumentRequest(
        String documentKey,
        String title,
        String sourceType,
        String sourceUri,
        Long courseId,
        String version,
        String content
) {
}
