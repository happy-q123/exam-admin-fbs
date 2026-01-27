package com.ai.service.agent.impl;

import com.ai.service.agent.AbstractAgentService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * description 判断其它agent结果的agent | 兜底agent | 判断结果
 * author zzq
 * date 2025/12/14 22:05
 * param
 * return
 */
@Service
public class JudgeResultAgent extends AbstractAgentService {
    public JudgeResultAgent(@Qualifier("zhiPuChatClientBuilder") ChatClient.Builder chatClientBuilder) {
        super(chatClientBuilder);
    }

    @Override
    protected void initProperties() {
        agentName="JudgeResultAgent";
        agentDescription="判断结果，是否需要重新使用模型生成";
        systemPrompt="请判断答案是否是在回答问题，请直接返回“是”或者“否”。";
    }

    @Override
    public Object execute(String query, String userId) {
        return null;
    }

    @Override
    public Object execute(String query) {
        return chatClient.prompt(query).call().chatClientResponse();
    }

    @Override
    public Object execute(String query, ChatOptions chatOptions) {
        return chatClient.prompt(query).options(chatOptions).call().chatClientResponse();
    }
}
