package com.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI会话主表
 * 对应表名：ai_conversation
 */
@Data
@Accessors(chain = true)
@TableName("ai_conversation")
public class AiConversation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话id
     * 注意：SQL中未定义自增序列，故使用 ASSIGN_ID (雪花算法) 自动生成 ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 会话创建的时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}