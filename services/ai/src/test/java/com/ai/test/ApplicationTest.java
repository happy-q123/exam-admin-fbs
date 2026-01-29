package com.ai.test;

import com.ai.feign.UserErrorQuestionFeignClient;
import com.ai.mapper.ChatMessageMapper;
import com.ai.service.agent.ChatService;
import com.ai.service.agent.AgentManager;
import com.ai.service.common.AiChatMessageService;
import com.domain.entity.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.AbsoluteIri;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest
public class ApplicationTest {
    @Resource
    private AgentManager agentManager;

    @Resource
    private ChatService chatService;

    @Resource
    private AiChatMessageService aiChatMessageService;

    @Resource
    private UserErrorQuestionFeignClient userErrorQuestionFeignClient;

    @Test
    public void test4(){
        List<ChatMessage> l=aiChatMessageService.lambdaQuery().list();
        for (ChatMessage chatMessage : l){
            log.warn("{}",chatMessage);
        }
    }

    @Test
    public void test() {
        String result = agentManager.doService("请用中文回答：你叫什么名字？");
        log.warn(result);
    }

    @Test
    public void testVectorSearch() {
        // 1. 模拟一个查询向量
        // 注意：这里的维度必须和数据库定义的维度一致！
        // 如果你之前插入的是 1536 维的随机数，这里也得弄个 1536 维的。
        // 为了演示方便，假设我们要查刚刚插入的那条 ID=... 的数据的向量

        // 先查出一条数据作为“种子”，用它的向量去搜它自己，理论上相似度应该是 1.0
        ChatMessage seed = aiChatMessageService.getById(5L); // 假设 ID 5 存在且有向量
        if (seed == null || seed.getEmbedding() == null) {
            log.warn("ID 5 的数据不存在或没有向量，无法测试");
            return;
        }

        // 2. 将 List<Double> 转换为 pgvector 需要的字符串格式 "[0.1, 0.2...]"
        ObjectMapper objectMapper = new ObjectMapper();
        String vectorStr;
        try {
            vectorStr = objectMapper.writeValueAsString(seed.getEmbedding());
        } catch (Exception e) {
            throw new RuntimeException("向量序列化失败", e);
        }

        // 3. 调用 Mapper 进行搜索 (假设找最相似的 3 条)
        // 这里的 baseMapper 是 Service 里的 mapper，如果 Service 没暴露，可以直接注入 ChatMessageMapper
        List<ChatMessage> results = ((ChatMessageMapper) aiChatMessageService.getBaseMapper())
                .searchByVector(vectorStr, 3);

        // 4. 打印结果
        log.info("========== 向量搜索结果 (Top 3) ==========");
        for (ChatMessage result : results) {
            log.info("ID: {}, 相似度: {}, 内容: {}",
                    result.getId(),
                    String.format("%.4f", result.getSimilarity()), // 格式化保留4位小数
                    result.getMessageContent());
        }
    }

    @Test
    public void testUserErrorQuestionFeignClient() {
        userErrorQuestionFeignClient.getErrorQuestionsByUserId(111L);
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
