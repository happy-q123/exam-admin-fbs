package com.exam.controller;

import com.domain.restful.RestResponse;
import com.domain.vo.UserErrorQuestionsVo;
import com.exam.service.UserOnlineExamAnswerService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ExamQuestionOptionController {
    private final UserOnlineExamAnswerService userOnlineExamAnswerService;

    public ExamQuestionOptionController(UserOnlineExamAnswerService userOnlineExamAnswerService) {
        this.userOnlineExamAnswerService = userOnlineExamAnswerService;
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
        Long jUserId = jwt.getClaim("userId");
        if(userId==null)
            return RestResponse.fail("token中无userId");
        Long userIdLong=null;
        if(userId.isBlank()){
            userIdLong=jUserId;
        }else
            userIdLong=Long.parseLong(userId);
        List<UserErrorQuestionsVo>l=userOnlineExamAnswerService.getUserAnswersByUserId(userIdLong);
        return RestResponse.success(l);
    }
}