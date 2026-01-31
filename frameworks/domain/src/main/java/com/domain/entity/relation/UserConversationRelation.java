package com.domain.entity.relation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 用户-ai对话关系表
 * 对应表名：user_conversation_relation
 */
@Data
@AllArgsConstructor
@Accessors(chain = true)
@TableName("user_conversation_relation")
public class UserConversationRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关系表ID
     * 注意：虽然数据库是复合主键(id, user_id)，但在MP中通常选一个唯一列作为逻辑主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 对话id
     */
    private Long conversationId;
}