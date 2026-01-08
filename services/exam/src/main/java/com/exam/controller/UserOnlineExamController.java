package com.exam.controller;

import com.domain.dto.UserOnlineExamAnswerDto;
import com.domain.dto.UserOnlineExamOptionsDto;
import com.domain.restful.RestResponse;
import com.exam.service.OnlineExamService;
import com.exam.service.UserOnlineExamAnswerService;
import com.exam.service.UserOnlineExamOptionsService;
import org.springframework.http.ResponseEntity;
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
public class UserOnlineExamController {
    private final UserOnlineExamAnswerService userOnlineExamAnswerService;
    private final UserOnlineExamOptionsService userOnlineExamOptionsService;
    private final OnlineExamService onlineExamService;
    public UserOnlineExamController(UserOnlineExamAnswerService userOnlineExamAnswerService,
                                    UserOnlineExamOptionsService userOnlineExamOptionsService, OnlineExamService onlineExamService) {
        this.userOnlineExamAnswerService = userOnlineExamAnswerService;
        this.userOnlineExamOptionsService = userOnlineExamOptionsService;
        this.onlineExamService = onlineExamService;
    }

    /**
     * description 保存用户在线考试行为
     * author zzq
     * date 2026/1/7 17:10
     * todo 确保幂等性
     */
    @PostMapping("/saveUserOnlineExamOption")
    public RestResponse saveUserOnlineExamOption(@AuthenticationPrincipal Jwt jwt
            ,@RequestBody UserOnlineExamOptionsDto userOnlineExamOptionsDto){

        Long userId = jwt.getClaim("userId");
        if(userId==null)
            return RestResponse.fail("token中无userId");

        if (!Objects.equals(userOnlineExamOptionsDto.getUserId(), userId))
            throw new RuntimeException("token用户id和请求体中用户id不一致");

        userOnlineExamOptionsService.saveUserOnlineExamOption(userOnlineExamOptionsDto);

        return RestResponse.success("保存成功");
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

    /**
     * description 用户请求进入考试
     * 检查用户是否报名->检查考试是否过期->检查是否初次进入->
     *                                  检查是否达到考试的最大重进次数->
     *
     * author zzq
     * date 2026/1/8 10:59
     */
    @PostMapping("/acquireEnterExam")
    public RestResponse<String> acquireEnterExam(@AuthenticationPrincipal Jwt jwt,
            @RequestBody UserOnlineExamOptionsDto userOnlineExamAnswerDto) {

        Long userId = jwt.getClaim("userId");
        if(userId==null)
            return RestResponse.fail("token中无userId");

        if (!Objects.equals(userOnlineExamAnswerDto.getUserId(), userId))
            throw new RuntimeException("token用户id和请求体中用户id不一致");
        onlineExamService.enterExam(userId, userOnlineExamAnswerDto.getExamId(), userOnlineExamAnswerDto.getOptionTime());
        return RestResponse.success("成功");
    }

    @PostMapping("/exitOnlineExam")
    public RestResponse<String> exitOnlineExam(@AuthenticationPrincipal Jwt jwt,
            @RequestBody UserOnlineExamOptionsDto userOnlineExamAnswerDto) {
        Long userId = jwt.getClaim("userId");
        if(userId==null)
            return RestResponse.fail("token中无userId");

        if (!Objects.equals(userOnlineExamAnswerDto.getUserId(), userId))
            throw new RuntimeException("token用户id和请求体中用户id不一致");

        onlineExamService.processUserDropOnline(userOnlineExamAnswerDto);
        return RestResponse.success("成功");
    }
}
