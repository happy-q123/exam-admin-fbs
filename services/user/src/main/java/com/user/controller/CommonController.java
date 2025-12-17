package com.user.controller;

import com.alibaba.nacos.api.utils.StringUtils;
import com.domain.dto.UserDto;
import com.user.service.UserOptionService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/common")
public class CommonController {

    @Resource
    private UserOptionService userOptionService;

    @RequestMapping("/getUserByUsername/{username}")
    public Object getUserByUsername(@PathVariable("username") String username){
        return userOptionService.getUserForLogin(username);
    }

    @PostMapping("/register")
    public Object register(@RequestBody UserDto userDto){
        return userOptionService.registerUser(userDto);
    }

}
