package com.ai.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;

public class ConversationIdAdvisor implements BaseAdvisor {
    private int order=0;
    private String conversationId="userId";

    //BaseChatMemoryAdvisor类默认获取conversationId的key
    private final String defaultConversationId="chat_memory_conversation_id";


    public ConversationIdAdvisor(int order) {
        this.order=order;
    }

    public ConversationIdAdvisor(int order, String conversationId) {
        this.order=order;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
//        chatClientRequest.context().put(defaultConversationId,conversationId);
        chatClientRequest.context().put(defaultConversationId,"userId");
        chatClientRequest.context().put("user-query",chatClientRequest.prompt().getUserMessage().getText());
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
