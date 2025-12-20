package com.user.controller;

import com.alibaba.nacos.api.utils.StringUtils;
import com.domain.dto.UserDto;
import com.domain.restful.RestResponse;
import com.user.service.UserOptionService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/common")
public class CommonController {

    @Resource
    private UserOptionService userOptionService;

    @RequestMapping("/getUserByUsername/{username}")
    public RestResponse getUserByUsername(@PathVariable("username") String username){
        UserDto userDto = userOptionService.getUserForLogin(username);
        return RestResponse.success(userDto);
    }

    @PostMapping("/register")
    public RestResponse register(@RequestBody UserDto userDto){
        String result=userOptionService.registerUser(userDto)? "注册成功": "注册失败";
        return RestResponse.success(result);
    }

}
