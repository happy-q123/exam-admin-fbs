package com.ai.service;

import com.ai.service.agent.impl.ChatMemoryAgent;
import com.ai.service.agent.impl.JudgeResultAgent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService{
    @Resource
    private ChatMemoryAgent chatMemoryAgent;

    @Resource
    private JudgeResultAgent judgeResultAgent;

    @Override
    public Object memoryChatWithJudge(String query) {
        ChatClientResponse chatClientResponse = (ChatClientResponse) chatMemoryAgent.execute(query);
        String answer = null;
        if (chatClientResponse.chatResponse() != null) {
            answer=chatClientResponse.chatResponse().getResult().getOutput().getText();
        }
        String userQuery= chatClientResponse.context().get("user-query").toString();
        PromptTemplate promptTemplate = new PromptTemplate("问题：{userQuery}，\n 答案：{answer}");
        String result="";
        if (answer != null) {
            result=promptTemplate.render(Map.of("answer", answer,"userQuery", userQuery));
        }
        log.info("回答agent结果：{}", result);

        ZhiPuAiChatOptions zhiPuAiChatOptions = new ZhiPuAiChatOptions();
        zhiPuAiChatOptions.setModel(ZhiPuAiApi.ChatModel.GLM_4_5_Air.value);
        zhiPuAiChatOptions.setTemperature(0.7);
        ChatClientResponse judgeResult = (ChatClientResponse) judgeResultAgent.execute(result,zhiPuAiChatOptions);
        return judgeResult;
    }

    @Override
    public Object memoryChatFlow(String query) {
        ChatClientResponse chatClientResponse = (ChatClientResponse) chatMemoryAgent.execute(query);
        String answer = null;
        if (chatClientResponse.chatResponse() != null) {
            answer=chatClientResponse.chatResponse().getResult().getOutput().getText();
        }
        String userQuery= chatClientResponse.context().get("user-query").toString();
        PromptTemplate promptTemplate = new PromptTemplate("问题：{userQuery}，\n 答案：{answer}");
        String result="";
        if (answer != null) {
            result=promptTemplate.render(Map.of("answer", answer,"userQuery", userQuery));
        }
        log.info("回答agent结果：{}", result);

        ZhiPuAiChatOptions zhiPuAiChatOptions = new ZhiPuAiChatOptions();
        zhiPuAiChatOptions.setModel(ZhiPuAiApi.ChatModel.GLM_4_5_Air.value);
        zhiPuAiChatOptions.setTemperature(0.7);
        ChatClientResponse judgeResult = (ChatClientResponse) judgeResultAgent.execute(result,zhiPuAiChatOptions);


        if(judgeResult.chatResponse().getResult().getOutput().getText().equals("否")){
            String query1="原问题："+query+"。这个问题上一次回答的不好，请重新回答。上一次回答是："+result;
            ChatClientResponse chatClientResponse1 = (ChatClientResponse) chatMemoryAgent.execute(query1);
            answer = null;
            if (chatClientResponse.chatResponse() != null) {
                answer=chatClientResponse.chatResponse().getResult().getOutput().getText();
            }
            userQuery= chatClientResponse.context().get("user-query").toString();
            promptTemplate = new PromptTemplate("问题：{userQuery}，\n 答案：{answer}");
            result="";
            if (answer != null) {
                result=promptTemplate.render(Map.of("answer", answer,"userQuery", userQuery));
            }
            log.info("再次回答agent结果：{}", result);
        }

        return judgeResult;
    }
}
