package com.ai.service.common;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domain.entity.relation.UserConversationRelation;

public interface UserConversationRelationService extends IService<UserConversationRelation> {

    default boolean belongsTo(Long userId, Long conversationId) {
        return userId != null && conversationId != null &&
                lambdaQuery().eq(UserConversationRelation::getUserId, userId)
                        .eq(UserConversationRelation::getConversationId, conversationId)
                        .exists();
    }
}
