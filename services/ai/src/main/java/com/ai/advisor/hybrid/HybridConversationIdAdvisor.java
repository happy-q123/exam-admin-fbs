package com.ai.advisor.hybrid;

import cn.hutool.core.lang.Assert;
import com.ai.service.common.AiChatComposeService;
import com.ai.service.common.UserConversationRelationService;
import com.domain.entity.relation.UserConversationRelation;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;

import java.util.Map;

public class HybridConversationIdAdvisor implements BaseAdvisor {
    private int order=0;
    private final AiChatComposeService aiChatComposeService;


    //BaseChatMemoryAdvisor类默认获取conversationId的key
//    private final String defaultConversationId="chat_memory_conversation_id";
    private final String defaultConversationId="conversationId";


    public HybridConversationIdAdvisor(int order,AiChatComposeService aiChatComposeService) {
        this.order=order;
        this.aiChatComposeService = aiChatComposeService;
    }

    public HybridConversationIdAdvisor(int order, String conversationId, AiChatComposeService aiChatComposeService) {
        this.order=order;
        this.aiChatComposeService = aiChatComposeService;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        // 获取上下文引用
        Map<String, Object> context = chatClientRequest.context();
        Assert.notNull(context, "context is null");

        String userId=String.valueOf(context.get("userId"));
        Assert.notNull(userId, "userId is null");

        Object conversationIdObj = context.get("conversationId");
        if (conversationIdObj == null){
            Long conversationId=aiChatComposeService.createConversation(Long.valueOf(userId));
            context.put(defaultConversationId,conversationId);
        }else {
            //todo 检查传进来的id是否存在，不存在就按照该id建立一个新会话，方便测试

        }
        chatClientRequest.context().put("userId",chatClientRequest.context().get("userId"));
        chatClientRequest.context().put("userQuery",chatClientRequest.prompt().getUserMessage().getText());
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return order;
    }
}
