package com.ai.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.Prompt;

@Slf4j
public class InformationAdvisor implements BaseAdvisor {
    private final int order;

    public InformationAdvisor(int order) {
        this.order = order;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        System.out.println("请求前置拦截器");
        Prompt p=chatClientRequest.prompt();
        System.out.println("Prompt信息如下：");
        System.out.println("System Message" + p.getSystemMessage().getText());
        System.out.println("User Messages" + p.getUserMessage().getText());

//        System.out.println("Context信息如下：");
//        Map<String, Object> contextMap=chatClientRequest.context();
//        contextMap.forEach((k,v)->System.out.println(k+":"+v));

        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        System.out.println("回复信息");
        System.out.println(chatClientResponse.context());
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return order;
    }
}
