package com.user.controller;

import com.alibaba.nacos.api.utils.StringUtils;
import com.domain.dto.UserDto;
import com.user.service.UserOptionService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/common")
public class CommonController {

    @Resource
    private UserOptionService userOptionService;

    @RequestMapping("/getUserByUsername/{username}")
    public Object getUserByUsername(@PathVariable("username") String username){
        if(StringUtils.isBlank(username)){
            return "用户名不能为空";
        }
        return userOptionService.getUserForLogin(username);
    }
}
