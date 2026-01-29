package com.domain.entity.relation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 会话-会话消息关联表
 * 对应表名：conversation_message_relation
 */
@Data
@Accessors(chain = true)
@TableName("conversation_message_relation")
public class ConversationMessageRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 会话id
     */
    private Long conversationId;

    /**
     * 会话里消息的id
     */
    private Long messageId;
}