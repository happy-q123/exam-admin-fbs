package com.ai.controller;

import com.ai.dto.KnowledgeDocumentRequest;
import com.ai.service.common.LocalRagService;
import com.domain.annotation.Audit;
import com.domain.entity.LocalRag;
import com.domain.restful.RestResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 课程知识库入库接口：原文切片后同时写入 PGVector（持久化）和 Redis Stack（热检索）。
 */
@RestController
@RequestMapping("/knowledge-base")
@PreAuthorize("@roleGuard.isTeacherOrAdmin(authentication)")
public class KnowledgeBaseController {
    private static final int CHUNK_SIZE = 1200;
    private static final int CHUNK_OVERLAP = 120;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final VectorStore pgVectorStore;
    private final VectorStore ragVectorStore;
    private final EmbeddingModel embeddingModel;
    private final LocalRagService localRagService;

    public KnowledgeBaseController(JdbcTemplate jdbcTemplate,
                                   ObjectMapper objectMapper,
                                   @Qualifier("pgVectorStore") VectorStore pgVectorStore,
                                   @Qualifier("ragVectorStore") VectorStore ragVectorStore,
                                   @Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel,
                                   LocalRagService localRagService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.pgVectorStore = pgVectorStore;
        this.ragVectorStore = ragVectorStore;
        this.embeddingModel = embeddingModel;
        this.localRagService = localRagService;
    }

    @GetMapping("/documents")
    public RestResponse<List<Map<String, Object>>> listDocuments() {
        return RestResponse.success(jdbcTemplate.queryForList("""
                SELECT id, document_key AS "documentKey", title, source_type AS "sourceType",
                       version, status, review_status AS "reviewStatus", created_time AS "createdTime",
                       updated_time AS "updatedTime"
                FROM ai_knowledge_document ORDER BY updated_time DESC
                """));
    }

    @Audit("知识库文档入库")
    @PostMapping("/documents")
    @Transactional(rollbackFor = Exception.class)
    public RestResponse<Map<String, Object>> ingest(@AuthenticationPrincipal Jwt jwt,
                                                     @RequestBody KnowledgeDocumentRequest request) {
        if (request == null || !StringUtils.hasText(request.documentKey())
                || !StringUtils.hasText(request.title()) || !StringUtils.hasText(request.content())) {
            throw new IllegalArgumentException("文档标识、标题和内容不能为空");
        }
        if (request.content().length() > 2_000_000) {
            throw new IllegalArgumentException("单个文档不能超过 2MB");
        }

        String contentHash = sha256(request.content());
        Long operatorId = currentUserId(jwt);
        jdbcTemplate.update("""
                INSERT INTO ai_knowledge_document(document_key, title, source_type, source_uri, course_id,
                    version, content_hash, status, review_status, created_by, updated_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'PUBLISHED', 'APPROVED', ?, CURRENT_TIMESTAMP)
                ON CONFLICT (document_key) DO UPDATE SET title = EXCLUDED.title,
                    source_type = EXCLUDED.source_type, source_uri = EXCLUDED.source_uri,
                    course_id = EXCLUDED.course_id, version = EXCLUDED.version,
                    content_hash = EXCLUDED.content_hash, status = 'PUBLISHED',
                    review_status = 'APPROVED', updated_time = CURRENT_TIMESTAMP
                """, request.documentKey().trim(), request.title().trim(), defaultValue(request.sourceType(), "MANUAL"),
                request.sourceUri(), request.courseId(), defaultValue(request.version(), "v1"), contentHash, operatorId);
        Long documentId = jdbcTemplate.queryForObject(
                "SELECT id FROM ai_knowledge_document WHERE document_key = ?", Long.class, request.documentKey().trim());
        if (documentId == null) throw new IllegalStateException("知识库文档保存失败");

        List<String> previousKeys = jdbcTemplate.queryForList(
                "SELECT chunk_key FROM ai_knowledge_chunk WHERE document_id = ?", String.class, documentId);
        deleteQuietly(pgVectorStore, previousKeys);
        deleteQuietly(ragVectorStore, previousKeys);
        jdbcTemplate.update("DELETE FROM ai_knowledge_chunk WHERE document_id = ?", documentId);

        List<Document> documents = new ArrayList<>();
        List<LocalRag> fallbackRows = new ArrayList<>();
        List<String> chunks = split(request.content());
        for (int index = 0; index < chunks.size(); index++) {
            String chunkKey = request.documentKey().trim() + "#" + (index + 1);
            String text = chunks.get(index);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("documentId", documentId);
            metadata.put("documentKey", request.documentKey().trim());
            metadata.put("source", request.title().trim());
            if (request.courseId() != null) metadata.put("courseId", request.courseId());
            documents.add(Document.builder().id(chunkKey).text(text).metadata(metadata).build());
            fallbackRows.add(new LocalRag().setRagSource(request.documentKey().trim()).setContent(text)
                    .setCreatedTime(LocalDateTime.now()));
            jdbcTemplate.update("""
                    INSERT INTO ai_knowledge_chunk(document_id, chunk_key, chunk_order, content, metadata,
                        course_id, content_hash, embedding_model, review_status)
                    VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?, 'APPROVED')
                    """, documentId, chunkKey, index + 1, text, json(metadata), request.courseId(),
                    sha256(text), "embeddinggemma:latest");
        }

        boolean pgStored = addQuietly(pgVectorStore, documents);
        boolean redisStored = addQuietly(ragVectorStore, documents);
        boolean localStored = false;
        try {
            // 保留旧 local_rag 作为数据库降级检索路径。
            localRagService.remove(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LocalRag>()
                    .eq(LocalRag::getRagSource, request.documentKey().trim()));
            fallbackRows.forEach(row -> row.setEmbedding(toDoubleList(embeddingModel.embed(row.getContent()))));
            localStored = localRagService.saveBatch(fallbackRows);
        } catch (RuntimeException ignored) {
            // 新的 PGVector/Redis 链路已经成功时，兼容表失败不阻断入库。
        }

        if (!pgStored && !redisStored && !localStored) {
            throw new IllegalStateException("向量存储不可用，知识文档未完成索引");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("documentId", documentId);
        result.put("documentKey", request.documentKey().trim());
        result.put("chunkCount", chunks.size());
        result.put("pgVectorStored", pgStored);
        result.put("redisVectorStored", redisStored);
        result.put("fallbackStored", localStored);
        return RestResponse.success(result);
    }

    private List<String> split(String content) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(content.length(), start + CHUNK_SIZE);
            chunks.add(content.substring(start, end).trim());
            if (end == content.length()) break;
            start = Math.max(start + 1, end - CHUNK_OVERLAP);
        }
        return chunks.stream().filter(StringUtils::hasText).toList();
    }

    private boolean addQuietly(VectorStore store, List<Document> documents) {
        try { store.add(documents); return true; } catch (RuntimeException ignored) { return false; }
    }

    private void deleteQuietly(VectorStore store, List<String> ids) {
        if (ids.isEmpty()) return;
        try { store.delete(ids); } catch (RuntimeException ignored) { }
    }

    private String json(Map<String, Object> metadata) {
        try { return objectMapper.writeValueAsString(metadata); }
        catch (JsonProcessingException exception) { return "{}"; }
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("内容摘要计算失败", exception);
        }
    }

    private List<Double> toDoubleList(float[] values) {
        List<Double> result = new ArrayList<>(values.length);
        for (float value : values) result.add((double) value);
        return result;
    }

    private Long currentUserId(Jwt jwt) {
        if (jwt == null || jwt.getClaim("userId") == null) throw new IllegalArgumentException("登录用户不存在");
        Object value = jwt.getClaim("userId");
        return value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
    }
}
