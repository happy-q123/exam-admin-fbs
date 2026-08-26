package com.ai.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;

@Slf4j
public class InformationAdvisor implements BaseAdvisor {
    private final int order;

    public InformationAdvisor(int order) {
        this.order = order;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        log.debug("AI request prepared: systemChars={}, userChars={}, contextKeys={}",
                chatClientRequest.prompt().getSystemMessage() == null ? 0
                        : chatClientRequest.prompt().getSystemMessage().getText().length(),
                chatClientRequest.prompt().getUserMessage() == null ? 0
                        : chatClientRequest.prompt().getUserMessage().getText().length(),
                chatClientRequest.context().keySet());
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        log.debug("AI response received: hasResponse={}", chatClientResponse != null);
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return order;
    }
}
