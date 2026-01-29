package com.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.domain.handler.PostgreSQLVectorTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户对话历史与向量存储表
 * 对应表名：chat_messages
 */
@Data
@Accessors(chain = true)
@TableName(value = "chat_message", autoResultMap = true) // autoResultMap = true 对 TypeHandler 很重要
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 对话内容的明文存储
     */
    private String messageContent;

    /**
     * 对话内容的向量值 (Embedding)，用于语义检索
     * 注意：这里指定了自定义的 TypeHandler
     */
    @TableField(typeHandler = PostgreSQLVectorTypeHandler.class)
    private List<Double> embedding;

    /**
     * 消息创建时间，带时区
     */
    @TableField(fill = FieldFill.INSERT) // 如果希望由 Java 填充时间，可配置自动填充；若完全依赖 DB default，可去掉此注解
    private LocalDateTime createdAt;

    /**
     * 角色类型：user(用户) 或 assistant(AI助手)，用于区分消息来源
     */
    private String role;

    /**
     * 搜索结果的相似度 (1.0 表示完全相同，0.0 表示完全无关)
     * 注意：这个字段数据库里没有，是查询时计算出来的
     */
    @TableField(exist = false)
    private Double similarity;
}