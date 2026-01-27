package com.ai.test;

import com.ai.service.ChatService;
import com.ai.service.agent.AgentManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class ApplicationTest {
    @Resource
    private AgentManager agentManager;

    @Resource
    private ChatService chatService;

    @Test
    public void test() {
        String result = agentManager.doService("请用中文回答：你叫什么名字？");
        log.warn(result);
    }

    @Test
    public void test2() {
        String result = agentManager.doService("ChatMemoryAgent","孙悟空的武器是什么？");
        log.warn(result);
    }


    @Test
    public void test3() {
//        String query1="孙悟空的武器是什么？";
//        ChatClientResponse judgeResult= (ChatClientResponse) chatService.memoryChatWithJudge(query1);
//        log.warn("query1：{}，最终结果：{}",query1,judgeResult.chatResponse().getResult().getOutput().getText());

        String query2="西游记中有迪迦奥特曼这个人物吗？";
//        ChatClientResponse judgeResult2= (ChatClientResponse) chatService.memoryChatWithJudge(query2);
        ChatClientResponse judgeResult2= (ChatClientResponse) chatService.memoryChatFlow(111L,query2);
        log.warn("query2：{}，最终结果：{}",query2,judgeResult2.chatResponse().getResult().getOutput().getText());
    }
}
