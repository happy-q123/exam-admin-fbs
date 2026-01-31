package com.ai.service.agent.impl;

import com.ai.advisor.ConversationIdAdvisor;
import com.ai.advisor.InformationAdvisor;
import com.ai.advisor.hybrid.HybridConversationIdAdvisor;
import com.ai.advisor.hybrid.HybridHistorySearchAdvisor;
import com.ai.service.agent.AbstractAgentService;
import com.ai.service.agent.ZhiPuRerankService;
import com.ai.service.common.AiChatComposeService;
import com.ai.service.common.AiChatMessageService;
import com.ai.service.common.UserConversationRelationService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class HybridCacheMemoryChatAgent extends AbstractAgentService {
    @Qualifier("messageVectorStore")
    @Resource
    VectorStore messageVectorStore;

    @Qualifier("ragVectorStore")
    @Resource
    VectorStore ragVectorStore;

    @Resource
    ZhiPuRerankService zhiPuRerankService;

    @Resource
    AiChatMessageService aiChatMessageService;

    @Resource
    AiChatComposeService aiChatComposeService;

    //如果参数名字为ollamaChatClientBuilder，则可以删掉@Qualifier("ollamaChatClientBuilder")的声明
    public HybridCacheMemoryChatAgent(@Qualifier("ollamaChatClientBuilder") ChatClient.Builder ollamaChatClientBuilder) {
        super(ollamaChatClientBuilder);
    }

    @Override
    protected void initAdvisors() {
        if (advisors==null)
            advisors=new ArrayList<>();

        //会话ID advisor
        HybridConversationIdAdvisor conversationIdAdvisor =
                new HybridConversationIdAdvisor(0,aiChatComposeService);
        advisors.add(conversationIdAdvisor);

        //历史搜索advisor
        HybridHistorySearchAdvisor hybridHistorySearchAdvisor = HybridHistorySearchAdvisor
                .builder(messageVectorStore,aiChatComposeService)
                .defaultTopK(10)
                .order(2)
//                .persistAndFilter("conversationId", "messageSource","userId")
                .build();
        advisors.add(hybridHistorySearchAdvisor);
        //信息打印advisor
        InformationAdvisor informationAdvisor = new InformationAdvisor(5);
        advisors.add(informationAdvisor);

    }

    @Override
    protected void initProperties() {
        agentName="HybridCacheMemoryChatAgent";
        agentDescription="混合redis和数据库的、能够记忆的的agent";
        systemPrompt="请尽量减少思考用时，回答时直接给出答案即可，不要回复如答案来源等无关的内容。";
    }

    @Override
    protected void initChatClient(ChatClient.Builder chatClientBuilder){
        ChatClient.Builder builder = chatClientBuilder.build().mutate().defaultSystem(this.systemPrompt);

        if (this.advisors != null && !this.advisors.isEmpty()) {
            builder.defaultAdvisors(this.advisors);
        }

        //构造tool
        this.chatClient = builder.defaultToolNames(
                        "userErrorQuestionsFunction",
                        "timeFunction",
                        "userIdFunction")
                .build();
    };

    @Override
    public Object execute(String query) {
        return chatClient.prompt(query).call().chatClientResponse();
    }

    @Override
    public Object execute(String query, String userId) {
        return chatClient.prompt(query)
                // 关键在这里：advisors(...) 不仅仅是添加新 Advisor，
                // 它主要用于给已有的 defaultAdvisors 传递运行时参数！
                .advisors(a ->
                        a.param("userId", userId)
                )
                .call()
                .chatClientResponse();
    }

    public Object execute(String query, String userId,String conversationId) {
        Long conversationIdLong = Long.valueOf(conversationId);
        return chatClient.prompt(query)
                // 关键在这里：advisors(...) 不仅仅是添加新 Advisor，
                // 它主要用于给已有的 defaultAdvisors 传递运行时参数！
                .advisors(a ->
                        a.param("userId", userId).param("conversationId", conversationIdLong)
                )
                .call()
                .chatClientResponse();
    }

    @Override
    public Object execute(String query, ChatOptions chatOptions) {
        return chatClient.prompt(query).options(chatOptions).call().chatClientResponse();
    }
}
