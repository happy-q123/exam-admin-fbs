package com.ai.service;

import com.ai.dto.ConversationMessageView;
import com.ai.dto.WrongQuestionAssistantRequest;
import com.ai.dto.WrongQuestionAssistantResponse;
import com.ai.feign.UserErrorQuestionFeignClient;
import com.ai.service.agent.impl.HybridCacheMemoryChatAgent;
import com.ai.service.agent.impl.JudgeResultAgent;
import com.ai.service.common.AiChatComposeService;
import com.ai.service.common.AiChatMessageService;
import com.ai.service.common.UserConversationRelationService;
import com.domain.restful.RestResponse;
import com.domain.vo.UserErrorQuestionsVo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 错题助手的显式工作流编排器。
 *
 * <p>每次请求依次执行上下文构建、知识检索/重排、答案生成和质量评估；评估不通过时最多
 * 重试两次，并返回可解释的兜底结果。这样 Agent 的职责和失败边界是可观察、可测试的。</p>
 */
@Service
public class WrongQuestionAssistantService {
    private static final int MAX_ATTEMPTS = 3;

    private final UserErrorQuestionFeignClient errorQuestionClient;
    private final HybridCacheMemoryChatAgent answerAgent;
    private final JudgeResultAgent qualityAgent;
    private final AiChatComposeService chatComposeService;
    private final AiChatMessageService chatMessageService;
    private final UserConversationRelationService relationService;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public WrongQuestionAssistantService(UserErrorQuestionFeignClient errorQuestionClient,
                                         HybridCacheMemoryChatAgent answerAgent,
                                         JudgeResultAgent qualityAgent,
                                         AiChatComposeService chatComposeService,
                                         AiChatMessageService chatMessageService,
                                         UserConversationRelationService relationService,
                                         ObjectMapper objectMapper,
                                         JdbcTemplate jdbcTemplate) {
        this.errorQuestionClient = errorQuestionClient;
        this.answerAgent = answerAgent;
        this.qualityAgent = qualityAgent;
        this.chatComposeService = chatComposeService;
        this.chatMessageService = chatMessageService;
        this.relationService = relationService;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public WrongQuestionAssistantResponse chat(Long userId, WrongQuestionAssistantRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("登录用户不存在");
        }
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }

        Long conversationId = request.conversationId();
        if (conversationId == null) {
            conversationId = chatComposeService.createConversation(userId);
        } else if (!relationService.belongsTo(userId, conversationId)) {
            throw new IllegalArgumentException("无权访问该会话");
        }

        UserErrorQuestionsVo wrongQuestion = findQuestion(userId, request.questionId());
        if (wrongQuestion == null && !StringUtils.hasText(request.query())) {
            throw new IllegalArgumentException("没有可用错题，请先完成一次考试或输入具体问题");
        }

        String questionContext = buildQuestionContext(wrongQuestion);
        String query = StringUtils.hasText(request.query())
                ? request.query().trim()
                : "请解释这道错题，说明正确思路、错误原因，并给出一道相似练习题。";
        String runId = UUID.randomUUID().toString();
        String answer = "暂时无法生成可靠讲解，请查看题目解析或联系教师。";
        boolean passed = false;
        String qualityNote = "模型服务不可用，已返回兜底提示";
        int attempts = 0;
        long startedAt = System.currentTimeMillis();
        recordRunStart(runId, userId, conversationId, request.questionId());

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            attempts = attempt;
            String prompt = buildPrompt(query, questionContext, attempt);
            long attemptStartedAt = System.currentTimeMillis();
            try {
                ChatClientResponse response = (ChatClientResponse) answerAgent.execute(
                        prompt, String.valueOf(userId), String.valueOf(conversationId));
                answer = contentOf(response);
                if (!StringUtils.hasText(answer)) {
                    qualityNote = "生成内容为空";
                    recordStep(runId, "answer-agent", attempt, "EMPTY", prompt, answer,
                            System.currentTimeMillis() - attemptStartedAt);
                    continue;
                }

                recordStep(runId, "answer-agent", attempt, "SUCCESS", prompt, answer,
                        System.currentTimeMillis() - attemptStartedAt);

                Evaluation evaluation = evaluate(query, questionContext, answer);
                passed = evaluation.passed();
                qualityNote = evaluation.note();
                recordStep(runId, "quality-agent", attempt, passed ? "PASS" : "FAIL",
                        query, qualityNote, System.currentTimeMillis() - attemptStartedAt);
                if (passed || attempt == MAX_ATTEMPTS) {
                    break;
                }
            } catch (Exception exception) {
                qualityNote = "第" + attempt + "次生成失败，已准备重试";
                recordStep(runId, "answer-agent", attempt, "ERROR", prompt,
                        exception.getClass().getSimpleName(), System.currentTimeMillis() - attemptStartedAt);
                if (attempt == MAX_ATTEMPTS) {
                    answer = "当前智能讲解服务暂时不可用。请先查看题目原解析，稍后再试。";
                }
            }
        }

        recordRunFinish(runId, attempts, passed, qualityNote, System.currentTimeMillis() - startedAt);

        return new WrongQuestionAssistantResponse(
                conversationId,
                wrongQuestion == null ? request.questionId() : wrongQuestion.getQuestionId(),
                answer,
                attempts,
                passed,
                qualityNote,
                extractCitations(answer),
                runId
        );
    }

    public List<ConversationMessageView> history(Long userId, Long conversationId) {
        if (!relationService.belongsTo(userId, conversationId)) {
            throw new IllegalArgumentException("无权访问该会话");
        }
        return chatMessageService.findConversationMessages(userId, conversationId).stream()
                .map(item -> new ConversationMessageView(
                        item.getMessageId(), item.getConversationId(), item.getUserContent(), item.getAiContent(),
                        item.getUserCreatedTime(), item.getAiCreatedTime()))
                .toList();
    }

    private UserErrorQuestionsVo findQuestion(Long userId, Long questionId) {
        RestResponse<List<UserErrorQuestionsVo>> response;
        try {
            response = errorQuestionClient.getErrorQuestionsByUserId(userId);
        } catch (RuntimeException exception) {
            // 错题服务短暂不可用时，允许用户继续追问通用知识，而不是让助手整条链路失败。
            return null;
        }
        List<UserErrorQuestionsVo> questions = response == null || response.getData() == null
                ? List.of() : response.getData();
        if (questionId != null) {
            return questions.stream().filter(item -> questionId.equals(item.getQuestionId())).findFirst().orElse(null);
        }
        return questions.isEmpty() ? null : questions.get(0);
    }

    private String buildQuestionContext(UserErrorQuestionsVo wrongQuestion) {
        if (wrongQuestion == null) {
            return "当前没有结构化错题上下文，请仅根据用户问题回答；不确定时明确说明不确定。";
        }
        try {
            return objectMapper.writeValueAsString(wrongQuestion);
        } catch (JsonProcessingException exception) {
            return String.valueOf(wrongQuestion);
        }
    }

    private String buildPrompt(String query, String questionContext, int attempt) {
        String retryHint = attempt == 1 ? "" : "这是修正请求。上一轮回答质量不足，请重新检索知识并检查推理，不要重复错误结论。";
        return "你是考试平台的错题讲解 Agent。\n" +
                "必须优先依据题目上下文和检索到的课程知识回答，禁止编造标准答案。\n" +
                "输出：1.正确结论 2.解题步骤 3.错误原因 4.知识点 5.一道相似练习题。\n" +
                "题目上下文：" + questionContext + "\n" +
                "用户问题：" + query + "\n" + retryHint;
    }

    private Evaluation evaluate(String query, String context, String answer) {
        try {
            ChatClientResponse response = (ChatClientResponse) qualityAgent.execute(
                    "请评估下面的错题讲解是否真正回答了用户问题，且没有脱离题目上下文。\n" +
                            "只输出 PASS 或 FAIL，随后用一句话说明原因。\n" +
                            "用户问题：" + query + "\n题目上下文：" + context + "\n讲解：" + answer);
            String text = contentOf(response);
            String normalized = text.toUpperCase();
            return new Evaluation(normalized.contains("PASS") || normalized.contains("是"), text);
        } catch (Exception exception) {
            // 评估模型不可用时，以最小确定性规则兜底，保证主流程仍可用。
            boolean usable = answer.length() >= 20 && !answer.contains("无法生成");
            return new Evaluation(usable, usable ? "评估模型不可用，已通过基础内容校验" : "内容过短或无法生成");
        }
    }

    private String contentOf(ChatClientResponse response) {
        if (response == null || response.chatResponse() == null || response.chatResponse().getResult() == null
                || response.chatResponse().getResult().getOutput() == null) {
            return "";
        }
        return response.chatResponse().getResult().getOutput().getText();
    }

    private List<String> extractCitations(String answer) {
        List<String> citations = new ArrayList<>();
        if (answer != null && answer.contains("知识点")) {
            citations.add("question-context");
        }
        return citations;
    }

    private record Evaluation(boolean passed, String note) {
    }

    private void recordRunStart(String runId, Long userId, Long conversationId, Long questionId) {
        executeQuietly(() -> jdbcTemplate.update("""
                INSERT INTO ai_agent_run(run_id, user_id, conversation_id, question_id, status, model_name, prompt_version)
                VALUES (?, ?, ?, ?, 'RUNNING', ?, ?)
                """, runId, userId, conversationId, questionId, "ollama", "wrong-question-v1"));
    }

    private void recordRunFinish(String runId, int attempts, boolean passed, String note, long latencyMs) {
        executeQuietly(() -> jdbcTemplate.update("""
                UPDATE ai_agent_run
                SET status = ?, attempts = ?, quality_passed = ?, quality_note = ?, latency_ms = ?, finished_time = CURRENT_TIMESTAMP
                WHERE run_id = ?
                """, passed ? "PASS" : "FALLBACK", attempts, passed, note, latencyMs, runId));
    }

    private void recordStep(String runId, String agentName, int attempt, String status,
                            String input, String output, long latencyMs) {
        executeQuietly(() -> jdbcTemplate.update("""
                INSERT INTO ai_agent_step(run_id, agent_name, step_order, status, input_summary, output_summary, latency_ms)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, runId, agentName, attempt, status, shorten(input), shorten(output), latencyMs));
    }

    private void executeQuietly(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ignored) {
            // 运行记录是增强能力，迁移尚未执行或数据库短暂不可用时不能阻断答题主链路。
        }
    }

    private String shorten(String value) {
        if (value == null) return "";
        return value.length() <= 2000 ? value : value.substring(0, 2000);
    }
}
