package com.ai.service.common.impl;

import com.ai.service.common.*;
import com.domain.dto.ChatMessageComposeDto;
import com.domain.entity.AiConversation;
import com.domain.entity.LocalRag;
import com.domain.entity.relation.ConversationMessageRelation;
import com.domain.entity.relation.UserConversationRelation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiChatComposeServiceImpl implements AiChatComposeService {
    private final AiConversationService aiConversationService;
    private final UserConversationRelationService userConversationRelationService;
    private final ConversationMessageRelationService conversationMessageRelationService;
    private final AiChatMessageService aiChatMessageService;
    private final LocalRagService localRagService;

    public AiChatComposeServiceImpl(AiConversationService aiConversationService, UserConversationRelationService userConversationRelationService, ConversationMessageRelationService conversationMessageRelationService, AiChatMessageService aiChatMessageService, LocalRagService localRagService) {
        this.aiConversationService = aiConversationService;
        this.userConversationRelationService = userConversationRelationService;
        this.conversationMessageRelationService = conversationMessageRelationService;
        this.aiChatMessageService = aiChatMessageService;
        this.localRagService = localRagService;
    }

    /**
     * description 向两个表插入记录，并返回新对话的id
     * author zzq
     * date 2026/1/31 20:56
    */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long createConversation(Long userId) {
        AiConversation conversation = new AiConversation(null, LocalDateTime.now());
        aiConversationService.save(conversation);
        Long conversationId = conversation.getId();

        userConversationRelationService.save(new UserConversationRelation(null, userId, conversationId));

        return conversationId;
    }

    /**
     * description 插入一条新的消息。插入ai聊天消息表和会话消息表。
     * author zzq
     * date 2026/1/31 21:09
     */
    @Transactional
    @Override
    public Long createMessage(Long conversationId,String userContent, LocalDateTime userCreateTime, String aiContent,
                              LocalDateTime aiCreateTime) {
        Long messageId=aiChatMessageService.saveChatPair(userContent, userCreateTime, aiContent, aiCreateTime);
        conversationMessageRelationService.save(new ConversationMessageRelation(null, conversationId, messageId));
        return messageId;
    }

    @Override
    public List<ChatMessageComposeDto> searchSimilarMessages(Long userId, Long conversationId, String query, int limit) {
        return aiChatMessageService.searchSimilarMessages(userId, conversationId, query, limit);
    }

    @Override
    public List<LocalRag> searchSimilarLocalRag(String query, @Nullable List<String> sources, @Nullable int limit) {
        return localRagService.searchSimilarRag(query, sources, limit);
    }
}
