package com.ai.utils;

import com.domain.entity.LocalRag;
import org.springframework.ai.document.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LocalRagMetaDataUtil {
    public static final String META_ID = "id";
    public static final String META_SOURCE = "ragSource"; // 对应 filter 中的 key
    public static final String META_CREATE_TIME = "createdTime";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 将数据库实体列表批量转换为 Document
     */
    public static List<Document> toDocuments(List<LocalRag> ragList) {
        if (ragList == null) return List.of();
        return ragList.stream()
                .map(LocalRagMetaDataUtil::toDocument)
                .collect(Collectors.toList());
    }

    /**
     * 将单个 LocalRag 实体转换为 Document (用于存入 Redis 或直接使用)
     */
    public static Document toDocument(LocalRag rag) {
        // 格式化时间
        String createTimeStr = formatTime(rag.getCreatedTime());

        // 组装 Metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(META_ID, rag.getId());
        metadata.put(META_SOURCE, rag.getRagSource() != null ? rag.getRagSource() : "knowledge");
        metadata.put(META_CREATE_TIME, createTimeStr);

        // 构造 Document
        // 注意：这里假设 LocalRag 有 getContent() 方法，如果没有请根据实际字段修改
        return Document.builder()
//                .id(String.valueOf(rag.getId())) // 显式设置 Document ID，防止重复插入生成不同的 ID
                .text(rag.getContent())
                .metadata(metadata)
                .build();
    }

    private static String formatTime(LocalDateTime time) {
        return time != null ? time.format(TIME_FORMATTER) : "";
    }
}
