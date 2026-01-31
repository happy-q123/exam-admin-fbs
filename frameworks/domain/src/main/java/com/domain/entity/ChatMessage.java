package com.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.domain.handler.PostgreSQLVectorTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户与AI的问答对历史记录表
 * 对应表名：chat_message
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@TableName(value = "chat_message", autoResultMap = true)
public class ChatMessage implements Serializable {

    public ChatMessage(String userContent, LocalDateTime userCreateTime, String aiContent, LocalDateTime aiCreateTime){
        this.userContent = userContent;
        this.userCreatedTime = userCreateTime;
        this.aiContent = aiContent;
        this.aiCreatedTime = aiCreateTime;
    }

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    // ================= 用户提问部分 =================

    /**
     * 用户发送的消息原文
     */
    private String userContent;

    /**
     * 用户消息的向量值 (768维)
     * 必须指定 TypeHandler 以处理 PGvector 格式
     */
    @TableField(typeHandler = PostgreSQLVectorTypeHandler.class)
    private List<Double> userEmbedding;

    /**
     * 用户发送消息的时间
     * 对应数据库 user_created_at
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime userCreatedTime;

    // ================= AI 回复部分 =================

    /**
     * AI回复的消息原文
     */
    private String aiContent;

    /**
     * AI回复的向量值 (768维)
     */
    @TableField(typeHandler = PostgreSQLVectorTypeHandler.class)
    private List<Double> aiEmbedding;

    /**
     * AI完成回复的时间
     * 对应数据库 ai_created_at
     */
    private LocalDateTime aiCreatedTime;

    // ================= 辅助字段 =================

    /**
     * 搜索结果的相似度 (1.0 表示完全相同)
     * 注意：这是一个计算字段，数据库表中不存在
     * 检索时，可能会计算与 userEmbedding 的相似度，也可能是与 aiEmbedding 的相似度
     */
    @TableField(exist = false)
    private Double similarity;
}