package com.exam.controller;

import com.domain.entity.Exam;
import com.domain.entity.ProctorEvent;
import com.domain.restful.RestResponse;
import com.exam.service.ExamService;
import com.exam.service.ProctorEventService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/proctor")
@PreAuthorize("@roleGuard.isTeacherOrAdmin(authentication)")
public class ProctorEventController {
    private final ProctorEventService eventService;
    private final ExamService examService;

    public ProctorEventController(ProctorEventService eventService, ExamService examService) {
        this.eventService = eventService;
        this.examService = examService;
    }

    @GetMapping("/events")
    public RestResponse<List<ProctorEvent>> events(@AuthenticationPrincipal Jwt jwt,
                                                   @RequestParam Long examId) {
        Exam exam = examService.getById(examId);
        if (exam == null) throw new IllegalArgumentException("考试不存在");
        if (!isAdmin(jwt) && !currentUserId(jwt).equals(exam.getCreator())) {
            return RestResponse.fail(403, "无权查看该考试监考记录");
        }
        return RestResponse.success(eventService.lambdaQuery()
                .eq(ProctorEvent::getExamId, examId)
                .orderByDesc(ProctorEvent::getCreatedTime)
                .last("LIMIT 200").list());
    }

    private Long currentUserId(Jwt jwt) {
        if (jwt == null || jwt.getClaim("userId") == null) throw new IllegalArgumentException("登录用户不存在");
        Object value = jwt.getClaim("userId");
        return value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
    }

    private boolean isAdmin(Jwt jwt) {
        String role = jwt == null ? "" : jwt.getClaimAsString("role");
        return "admin".equalsIgnoreCase(role) || "ROLE_admin".equalsIgnoreCase(role);
    }
}
