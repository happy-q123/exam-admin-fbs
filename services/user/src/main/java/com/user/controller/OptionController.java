package com.user.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/option")
public class OptionController {
    @RequestMapping("/getHello")
    public String getUser(@AuthenticationPrincipal Jwt jwt){
        // 1. 获取用户名 (对应 Token 中的 "sub" 字段)
        String username = jwt.getSubject();

        // 2. 获取我们在 Auth Server 里自定义塞进去的 "userId"
        // 注意：getClaim 的返回值是 Object，可能需要强转或 toString
        Long userId = jwt.getClaim("userId");

        // 3. 获取其他信息 (比如 Token ID, 过期时间等)
        String tokenId = jwt.getId();

        // 4. 获取权限 Scope (对应 "scope" 字段)
        // 注意：这里拿的是原始字符串列表，如 ["openid", "order:read"]
        // 如果要看 Spring 转换后的权限对象，得去 SecurityContext 里拿
        List<String> scopes = jwt.getClaim("scope");
        return String.format("你好，用户 %s (ID: %d)，你的权限是: %s", username, userId, scopes);
    }


}
