package com.ai.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;

public record ConversationMessageView(
        @JsonSerialize(using = ToStringSerializer.class)
        Long messageId,
        @JsonSerialize(using = ToStringSerializer.class)
        Long conversationId,
        String userContent,
        String aiContent,
        LocalDateTime userCreatedTime,
        LocalDateTime aiCreatedTime
) {
}
