package com.ai.dto;

import java.time.LocalDateTime;

public record ConversationMessageView(
        Long messageId,
        Long conversationId,
        String userContent,
        String aiContent,
        LocalDateTime userCreatedTime,
        LocalDateTime aiCreatedTime
) {
}
