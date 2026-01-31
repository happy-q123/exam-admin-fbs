//package com.ai.service.agent;
//
//import com.ai.service.agent.impl.ChatMemoryAgent;
//import com.ai.service.agent.impl.JudgeResultAgent;
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.chat.client.ChatClientResponse;
//import org.springframework.ai.chat.prompt.PromptTemplate;
//import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
//import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
//import org.springframework.stereotype.Service;
//
//import java.util.Map;
//
//@Slf4j
//@Service
//public class ChatServiceImpl implements ChatService{
//    @Resource
//    private ChatMemoryAgent chatMemoryAgent;
//
//    @Resource
//    private JudgeResultAgent judgeResultAgent;
//
//    @Override
//    public Object memoryChatWithJudge(String query) {
//        ChatClientResponse chatClientResponse = (ChatClientResponse) chatMemoryAgent.execute(query);
//        String answer = null;
//        if (chatClientResponse.chatResponse() != null) {
//            answer=chatClientResponse.chatResponse().getResult().getOutput().getText();
//        }
//        String userQuery= chatClientResponse.context().get("user-query").toString();
//        PromptTemplate promptTemplate = new PromptTemplate("问题：{userQuery}，\n 答案：{answer}");
//        String result="";
//        if (answer != null) {
//            result=promptTemplate.render(Map.of("answer", answer,"userQuery", userQuery));
//        }
//        log.info("回答agent结果：{}", result);
//
//        ZhiPuAiChatOptions zhiPuAiChatOptions = new ZhiPuAiChatOptions();
//        zhiPuAiChatOptions.setModel(ZhiPuAiApi.ChatModel.GLM_4_5_Air.value);
//        zhiPuAiChatOptions.setTemperature(0.7);
//        ChatClientResponse judgeResult = (ChatClientResponse) judgeResultAgent.execute(result,zhiPuAiChatOptions);
//        return judgeResult;
//    }
//
//    //提供一个默认方法，使用Id为0
//    public Object memoryChatFlow(String query) {
//        return memoryChatFlow(0L,query);
//    }
//
//    @Override
//    public Object memoryChatFlow(Long userId, String query) {
//        // === 闭环配置 ===
//        final int MAX_RETRIES = 3; // 最大重试次数，防止无限循环消耗 Token
//        int currentAttempt = 0;
//
//        // 初始化变量
//        String currentQuery = query; // 当前发给 Agent 的提示词（第一轮就是用户原话）
//        ChatClientResponse finalResponse = null;
//        String lastAnswerText = "";
//
//        log.info("=== 开始闭环处理流程，原问题: {} ===", query);
//
//        while (currentAttempt <= MAX_RETRIES) {
//            // 1. 【执行】调用生成 Agent
//            log.info(">>> 第 {} 次尝试生成回答...", currentAttempt + 1);
////            finalResponse = (ChatClientResponse) chatMemoryAgent.execute(currentQuery);
//            finalResponse = (ChatClientResponse) chatMemoryAgent.execute(currentQuery,String.valueOf(userId));
//
//            // 提取回答文本 (封装了空指针保护)
//            lastAnswerText = extractAnswer(finalResponse);
//
//            // 2. 【准备评估素材】
//            // 注意：裁判永远是根据“原问题”和“当前回答”来判断，不能用重试的提示词干扰裁判
//            String contextForJudge = renderPrompt(query, lastAnswerText);
//
//            // 3. 【评估】调用裁判 Agent
//            ZhiPuAiChatOptions zhiPuAiChatOptions = new ZhiPuAiChatOptions();
//            // 裁判需要理性、严格，Temperature 设低一点
//            zhiPuAiChatOptions.setModel(ZhiPuAiApi.ChatModel.GLM_4_5_Air.value);
//            zhiPuAiChatOptions.setTemperature(0.1);
//
//            ChatClientResponse judgeResult = (ChatClientResponse) judgeResultAgent.execute(contextForJudge, zhiPuAiChatOptions);
//            String judgeVerdict = extractAnswer(judgeResult).trim(); // 去除可能的空格
//
//            log.info("<<< 第 {} 次评估结果: [{}]", currentAttempt + 1, judgeVerdict);
//
//            // 4. 【决策分支】
//            if ("是".equals(judgeVerdict)) {
//                log.info("=== 闭环结束：回答通过审核 ===");
//                return finalResponse; // 成功闭环，直接返回
//            }
//
//            // 5. 【准备下一次循环】
//            currentAttempt++;
//
//            if (currentAttempt > MAX_RETRIES) {
//                log.warn("=== 闭环结束：达到最大重试次数 ({})，返回最后一次结果 ===", MAX_RETRIES);
//                // 即使最后一次也是“否”，但也只能返回了（或者你可以选择抛出异常或返回兜底文案）
//                break;
//            }
//
//            // 6. 【修正】构建下一轮的输入 (核心步骤)
//            // 告诉 Agent：原本的问题是什么，它上次回答了什么，以及被判定为错误，要求重写。
//            currentQuery = String.format(
//                    "你之前的回答未通过审核。\n\n原问题：%s\n\n你上一次的回答：%s\n\n请反思该回答的不足之处，并重新给出一个更准确、更完善的答案。",
//                    query,
//                    lastAnswerText
//            );
//        }
//
//        return finalResponse;
//    }
//
//    /**
//     * 辅助方法：安全提取回答文本
//     */
//    private String extractAnswer(ChatClientResponse response) {
//        if (response != null && response.chatResponse() != null && response.chatResponse().getResult() != null) {
//            return response.chatResponse().getResult().getOutput().getText();
//        }
//        return "";
//    }
//
//    /**
//     * 辅助方法：渲染裁判提示词
//     */
//    private String renderPrompt(String userQuery, String answer) {
//        // 确保你的 PromptTemplate 能够处理可能的特殊字符
//        PromptTemplate promptTemplate = new PromptTemplate("问题：{userQuery}，\n 答案：{answer}\n 请判断上述答案是否解决了问题？仅回答“是”或“否”。");
//        return promptTemplate.render(Map.of("answer", answer, "userQuery", userQuery));
//    }
//}
