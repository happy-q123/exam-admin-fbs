package com.exam.controller;

import com.domain.restful.RestResponse;
import com.domain.entity.ErrorBook;
import com.domain.vo.UserErrorQuestionsVo;
import com.exam.service.ErrorBookService;
import com.exam.service.ExamService;
import com.exam.service.UserOnlineExamAnswerService;
import com.domain.entity.Exam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class ExamQuestionOptionController {
    private final UserOnlineExamAnswerService userOnlineExamAnswerService;
    private final ErrorBookService errorBookService;
    private final ExamService examService;

    public ExamQuestionOptionController(UserOnlineExamAnswerService userOnlineExamAnswerService,
                                        ErrorBookService errorBookService,
                                        ExamService examService) {
        this.userOnlineExamAnswerService = userOnlineExamAnswerService;
        this.errorBookService = errorBookService;
        this.examService = examService;
    }

    /**
     * description 获取用户的考试错题
     * author zzq
     * date 2026/01/28 17:51
     */
    //todo 转为question对象。
    @GetMapping("/getExamErrorQuestions/{userId}")
    public RestResponse<List<UserErrorQuestionsVo>> insertOne(@AuthenticationPrincipal Jwt jwt,
                                                        @PathVariable("userId") String userId) {
        Long jUserId = currentUserId(jwt);
        if(jUserId==null)
            return RestResponse.fail("token中无userId");
        Long userIdLong=null;
        if(userId.isBlank()){
            userIdLong=jUserId;
        }else
            userIdLong=Long.parseLong(userId);
        boolean admin = hasRole(jwt, "admin");
        boolean teacher = hasRole(jwt, "teacher");
        if (!jUserId.equals(userIdLong) && !teacher && !admin) {
            return RestResponse.fail(403, "只能查看自己的错题");
        }
        var errorBookQuery = errorBookService.lambdaQuery().eq(ErrorBook::getUserId, userIdLong);
        if (!jUserId.equals(userIdLong) && teacher && !admin) {
            Set<Long> managedExamIds = examService.lambdaQuery()
                    .select(Exam::getId)
                    .eq(Exam::getCreator, jUserId)
                    .list().stream()
                    .map(Exam::getId)
                    .collect(Collectors.toSet());
            if (managedExamIds.isEmpty()) return RestResponse.success(List.of());
            errorBookQuery.in(ErrorBook::getExamId, managedExamIds);
        }
        Set<String> errorKeys = errorBookQuery.list().stream()
                .map(item -> item.getExamId() + ":" + item.getQuestionId())
                .collect(Collectors.toSet());
        List<UserErrorQuestionsVo> l = userOnlineExamAnswerService.getUserAnswersByUserId(userIdLong)
                .stream()
                .filter(item -> errorKeys.contains(item.getExamId() + ":" + item.getQuestionId()))
                .toList();
        return RestResponse.success(l);
    }

    private Long currentUserId(Jwt jwt) {
        if (jwt == null || jwt.getClaim("userId") == null) return null;
        Object value = jwt.getClaim("userId");
        return value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
    }

    private boolean hasRole(Jwt jwt, String expected) {
        if (jwt == null) return false;
        String role = jwt.getClaimAsString("role");
        if (expected.equalsIgnoreCase(role) || ("ROLE_" + expected).equalsIgnoreCase(role)) return true;
        Object roles = jwt.getClaim("roles");
        if (roles instanceof java.util.Collection<?> collection
                && collection.stream().map(String::valueOf)
                .anyMatch(item -> expected.equalsIgnoreCase(item.replaceFirst("^ROLE_", "")))) {
            return true;
        }
        Object authorities = jwt.getClaim("authorities");
        return authorities instanceof java.util.Collection<?> collection
                && collection.stream().map(String::valueOf)
                .anyMatch(item -> expected.equalsIgnoreCase(item.replaceFirst("^ROLE_", "")));
    }
}
