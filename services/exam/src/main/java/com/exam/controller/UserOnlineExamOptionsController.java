package com.exam.controller;

import com.domain.dto.UserOnlineExamOptionsDto;
import com.domain.restful.RestResponse;
import com.exam.service.UserOnlineExamOptionsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
/**
 * description 用户在线考试行为操作
 * author zzq
 * date 2026/1/7 17:11
 */
@RestController
public class UserOnlineExamOptionsController {
    private final UserOnlineExamOptionsService userOnlineExamOptionsService;

    public UserOnlineExamOptionsController(UserOnlineExamOptionsService userOnlineExamOptionsService) {
        this.userOnlineExamOptionsService = userOnlineExamOptionsService;
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
}
