package com.ai.utils;

import com.domain.dto.ChatMessageComposeDto;
import org.springframework.ai.document.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * description 定义向redis-stack保存消息时的元数据，以及元数据转为document的方法
 * author zzq
 * date 2026/2/1 16:22
*/

public class ChatMessageMetaDataUtil {
    public static final String META_USER_ID = "userId";
    public static final String META_CONV_ID = "conversationId";
    public static final String META_USER_TIME = "userCreatedTime";
    public static final String META_AI_TIME = "aiCreatedTime";
    public static final String META_USER_CONTENT = "userContent";
    public static final String META_AI_CONTENT = "aiContent";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 构建用于保存到 Redis 的 Document
     */
    public Document toRedisDocument(String userText, String aiText,
                                    LocalDateTime userTime, LocalDateTime aiTime,
                                    String userId, String conversationId) {

        // 格式化时间
        String userTimeStr = formatTime(userTime);
        String aiTimeStr = formatTime(aiTime);

        // 组合文本 (用于向量计算和 RAG 上下文展示)
        String combinedText = String.format(
                "[%s] User: %s\n[%s] Assistant: %s",
                userTimeStr, userText,
                aiTimeStr, aiText
        );

        //组装 Metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(META_USER_ID, userId);
        metadata.put(META_CONV_ID, conversationId);
        metadata.put(META_USER_TIME, userTimeStr);
        metadata.put(META_AI_TIME, aiTimeStr);
        metadata.put(META_USER_CONTENT, userText); // 冗余存一份原文，方便后续不查库直接展示
        metadata.put(META_AI_CONTENT, aiText);
        metadata.put("type", "conversation_history");

        return Document.builder()
                .text(combinedText)
                .metadata(metadata)
                .build();
    }

    /**
     * 将数据库实体转换为 Document (用于 DB 降级检索)
     */
    public Document fromDbEntity(ChatMessageComposeDto msg) {
        String userTimeStr = formatTime(msg.getUserCreatedTime());
        String aiTimeStr = formatTime(msg.getAiCreatedTime());

        String content = String.format(
                "[%s] User: %s\n[%s] Assistant: %s",
                userTimeStr, msg.getUserContent(),
                aiTimeStr, msg.getAiContent()
        );

        return Document.builder()
                .text(content)
                .metadata(META_USER_ID, String.valueOf(msg.getUserId()))
                .metadata(META_CONV_ID, String.valueOf(msg.getConversationId()))
                .metadata(META_USER_TIME, userTimeStr)
                .metadata(META_AI_TIME, aiTimeStr)
                .metadata("source", "database_fallback") // 标记来源
                .build();
    }

    /**
     * 构建 Redis 过滤表达式
     */
    public String buildFilterExpression(String userId, String conversationId) {
        return String.format("%s == '%s' && %s == '%s'",
                META_USER_ID, userId,
                META_CONV_ID, conversationId);
    }

    private String formatTime(LocalDateTime time) {
        return time != null ? time.format(TIME_FORMATTER) : "";
    }
}
