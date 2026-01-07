package com.exam.controller;

import com.domain.dto.UserOnlineExamAnswerDto;
import com.domain.restful.RestResponse;
import com.exam.service.UserOnlineExamAnswerService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
/**
 * description 用户在线考试答案操作
 * author zzq
 * date 2026/1/7 17:11
 */
@RestController
public class UserOnlineExamAnswerController {
    private final UserOnlineExamAnswerService userOnlineExamAnswerService;

    public UserOnlineExamAnswerController(UserOnlineExamAnswerService userOnlineExamAnswerService) {
        this.userOnlineExamAnswerService = userOnlineExamAnswerService;
    }

    //保存用户在线考试答案，只针对某一个题
    @PostMapping("/saveOnlineExamAnswer")
    public RestResponse saveOnlineExamAnswer(@AuthenticationPrincipal Jwt jwt
            ,@RequestBody UserOnlineExamAnswerDto userOnlineExamAnswerDto){

        Long userId = jwt.getClaim("userId");
        if(userId==null)
            return RestResponse.fail("token中无userId");

        if (!Objects.equals(userOnlineExamAnswerDto.getUserId(), userId))
            throw new RuntimeException("token用户id和请求体中用户id不一致");

        userOnlineExamAnswerService.saveAnswer(userOnlineExamAnswerDto);
        return RestResponse.success("保存成功");
    }
}
