package com.exam.controller;

import com.domain.restful.RestResponse;
import com.domain.entity.ErrorBook;
import com.domain.vo.UserErrorQuestionsVo;
import com.exam.service.ErrorBookService;
import com.exam.service.UserOnlineExamAnswerService;
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

    public ExamQuestionOptionController(UserOnlineExamAnswerService userOnlineExamAnswerService,
                                        ErrorBookService errorBookService) {
        this.userOnlineExamAnswerService = userOnlineExamAnswerService;
        this.errorBookService = errorBookService;
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
        String role = jwt.getClaimAsString("role");
        if (!jUserId.equals(userIdLong)
                && !"teacher".equalsIgnoreCase(role)
                && !"admin".equalsIgnoreCase(role)) {
            return RestResponse.fail(403, "只能查看自己的错题");
        }
        Set<String> errorKeys = errorBookService.lambdaQuery()
                .eq(ErrorBook::getUserId, userIdLong)
                .list().stream()
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
}
