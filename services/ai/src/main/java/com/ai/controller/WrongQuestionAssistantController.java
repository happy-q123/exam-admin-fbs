package com.ai.controller;

import com.ai.dto.ConversationMessageView;
import com.ai.dto.WrongQuestionAssistantRequest;
import com.ai.dto.WrongQuestionAssistantResponse;
import com.ai.dto.WrongQuestionFeedbackRequest;
import com.ai.service.WrongQuestionAssistantService;
import com.domain.restful.RestResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/wrong-question-assistant")
public class WrongQuestionAssistantController {
    private final WrongQuestionAssistantService assistantService;
    private final JdbcTemplate jdbcTemplate;

    public WrongQuestionAssistantController(WrongQuestionAssistantService assistantService, JdbcTemplate jdbcTemplate) {
        this.assistantService = assistantService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/chat")
    public RestResponse<WrongQuestionAssistantResponse> chat(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody WrongQuestionAssistantRequest request) {
        return RestResponse.success(assistantService.chat(currentUserId(jwt), request));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public RestResponse<List<ConversationMessageView>> history(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("conversationId") Long conversationId) {
        return RestResponse.success(assistantService.history(currentUserId(jwt), conversationId));
    }

    @PostMapping("/feedback")
    public RestResponse<Boolean> feedback(@AuthenticationPrincipal Jwt jwt,
                                          @RequestBody WrongQuestionFeedbackRequest request) {
        Long userId = currentUserId(jwt);
        if (request == null || request.agentRunId() == null || request.agentRunId().isBlank()) {
            throw new IllegalArgumentException("运行记录不能为空");
        }
        String rating = request.rating() == null ? "" : request.rating().trim().toUpperCase(Locale.ROOT);
        if (!"POSITIVE".equals(rating) && !"NEGATIVE".equals(rating)) {
            throw new IllegalArgumentException("反馈类型不合法");
        }
        Integer owned = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ai_agent_run WHERE run_id = ? AND user_id = ?",
                Integer.class, request.agentRunId(), userId);
        if (owned == null || owned == 0) throw new IllegalArgumentException("运行记录不存在");
        jdbcTemplate.update("""
                INSERT INTO ai_answer_feedback(run_id, user_id, rating, reason)
                VALUES (?, ?, ?, ?)
                """, request.agentRunId(), userId, rating,
                request.reason() == null ? null : request.reason().trim());
        return RestResponse.success(true);
    }

    private Long currentUserId(Jwt jwt) {
        if (jwt == null || jwt.getClaim("userId") == null) {
            throw new IllegalArgumentException("登录用户不存在");
        }
        Object value = jwt.getClaim("userId");
        return value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
    }
}
