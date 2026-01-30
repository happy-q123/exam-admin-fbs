package com.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.domain.handler.PostgreSQLVectorTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 本地 RAG 知识库向量表
 * 对应表名：local_rag
 */
@Data
@Accessors(chain = true)
@TableName(value = "local_rag", autoResultMap = true) // 必须开启 autoResultMap 才能自动处理 TypeHandler
public class LocalRag implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 知识来源标识
     * 例如："运维手册_v1.pdf", "用户协议.docx"
     */
    private String messageSource;

    /**
     * 知识片段原文
     * 这段文字将被送给 AI 作为参考上下文
     */
    private String content;

    /**
     * 内容的向量值 (768维)
     */
    @TableField(typeHandler = PostgreSQLVectorTypeHandler.class)
    private List<Double> embedding;

    /**
     * 知识入库时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    // ================= 辅助字段 =================

    /**
     * 检索时的相似度
     * (数据库中不存在此列)
     */
    @TableField(exist = false)
    private Double similarity;
}