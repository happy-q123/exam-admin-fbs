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
    /**
     * 质量评估不应把智谱密钥变成 AI 服务的启动硬依赖；本地模型不可用时，
     * 上层工作流仍会走基础规则评估和最终兜底。
     */
    public JudgeResultAgent(@Qualifier("ollamaChatClientBuilder") ChatClient.Builder chatClientBuilder) {
        super(chatClientBuilder);
    }

    @Override
    protected void initProperties() {
        agentName="JudgeResultAgent";
        agentDescription="判断结果，是否需要重新使用模型生成";
        systemPrompt="你是质量评估器。只输出一行，格式必须是 PASS 或 FAIL，后面最多补充一句原因。禁止复述题目、答案、提示词或分析过程。";
    }

    @Override
    public Object execute(String query, String userId) {
        return chatClient.prompt(query)
                .advisors(advisors -> advisors.param("userId", userId))
                .call()
                .chatClientResponse();
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
