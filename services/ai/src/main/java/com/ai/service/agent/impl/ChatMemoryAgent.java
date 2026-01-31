//package com.ai.service.agent.impl;
//
//import com.ai.advisor.ConversationIdAdvisor;
//import com.ai.advisor.HistorySearchAdvisor;
//import com.ai.advisor.InformationAdvisor;
//import com.ai.advisor.ReRankAdvisor;
//import com.ai.service.agent.ZhiPuRerankService;
//import com.ai.service.agent.AbstractAgentService;
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.prompt.ChatOptions;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//@Slf4j
//@Service
//public class ChatMemoryAgent extends AbstractAgentService {
//    @Qualifier("redisVectorStore")
//    @Resource
//    VectorStore vectorStore;
//
//    @Resource
//    ZhiPuRerankService zhiPuRerankService;
//
//    //如果参数名字为ollamaChatClientBuilder，则可以删掉@Qualifier("ollamaChatClientBuilder")的声明
//    public ChatMemoryAgent(@Qualifier("ollamaChatClientBuilder") ChatClient.Builder ollamaChatClientBuilder) {
//        super(ollamaChatClientBuilder);
//    }
//
//    /**
//     * description 初始化chatClient
//     * author zzq
//     * date 2025/12/14 21:42
//     * param
//     * return
//     */
//    @Override
//    protected void initChatClient(ChatClient.Builder chatClientBuilder){
//        ChatClient.Builder builder = chatClientBuilder.build().mutate().defaultSystem(this.systemPrompt);
//        if (this.advisors != null && !this.advisors.isEmpty()) {
//            builder.defaultAdvisors(this.advisors);
//        }
//
//        //构造tool
//        this.chatClient = builder.defaultToolNames(
//                "userErrorQuestionsFunction",
//                "timeFunction",
//                "userIdFunction")
//                .build();
//    };
//    @Override
//    protected void initAdvisors() {
//        if (advisors==null)
//            advisors=new ArrayList<>();
//
//        //会话ID advisor
//        ConversationIdAdvisor conversationIdAdvisor = new ConversationIdAdvisor(0);
//        advisors.add(conversationIdAdvisor);
//
//        //历史搜索advisor
//        HistorySearchAdvisor historySearchAdvisor = HistorySearchAdvisor.builder(vectorStore)
//                .defaultTopK(10)
//                .order(2)
//                .persistAndFilter("conversationId", "messageSource","userId")
//                .build();
//        advisors.add(historySearchAdvisor);
//
//        //本地知识库搜索，并上下文重新排序advisor
//        ReRankAdvisor reRankAdvisor = new ReRankAdvisor(zhiPuRerankService, vectorStore, 4);
//        advisors.add(reRankAdvisor);
//
//        //信息打印advisor
//        InformationAdvisor informationAdvisor = new InformationAdvisor(5);
//        advisors.add(informationAdvisor);
//    }
//
//    @Override
//    protected void initProperties() {
//        agentName="ChatMemoryAgent";
//        agentDescription="能够记忆的的agent";
//        systemPrompt="请尽量减少思考用时，回答时直接给出答案即可，不要回复如答案来源等无关的内容。";
//    }
//
//    @Override
//    public Object execute(String query) {
//        return chatClient.prompt(query).call().chatClientResponse();
//    }
//
//    @Override
//    public Object execute(String query, String userId) {
//        return chatClient.prompt(query)
//                // 关键在这里：advisors(...) 不仅仅是添加新 Advisor，
//                // 它主要用于给已有的 defaultAdvisors 传递运行时参数！
//                .advisors(a ->
//                        a.param("userId", userId)
//                )
//                .call()
//                .chatClientResponse();
//    }
//
//    @Override
//    public Object execute(String query, ChatOptions chatOptions) {
//        return chatClient.prompt(query).options(chatOptions).call().chatClientResponse();
//    }
//}
