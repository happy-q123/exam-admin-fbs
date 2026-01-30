package com.domain.dto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.domain.handler.PostgreSQLVectorTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessageComposeDto {
    //对话id
    private Long conversationId;
    //用户id
    private Long userId;
    //会话里的消息id
    private Long messageId;

    /**
     * 用户发送的消息原文
     */
    private String userContent;

    /**
     * 用户消息的向量值 (768维)
     * 必须指定 TypeHandler 以处理 PGvector 格式
     */
    private List<Double> userEmbedding;

    /**
     * 用户发送消息的时间
     * 对应数据库 user_created_at
     */
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
