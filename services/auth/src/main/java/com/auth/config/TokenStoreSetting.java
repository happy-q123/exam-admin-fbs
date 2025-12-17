package com.auth.config;

import com.auth.dto.CustomSecurityUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

@Configuration
public class TokenStoreSetting {

    /**
     * description 往token里添加字段
     * author zzq
     * date 2025/12/17 16:58
     * param
     * return
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {
            // 通常只往 Access Token (访问令牌) 里塞，ID Token 也可以塞
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {

                // 获取当前登录的用户信息
                // context.getPrincipal() 返回的是 Authentication 对象
                Authentication principal = context.getPrincipal();

                // 安全判断：必须是我们自定义的 SecurityUser 才能取 ID
                // (因为如果是 Client Credentials 模式，principal 是 String 类型的 clientId)
                if (principal.getPrincipal() instanceof CustomSecurityUser user) {
                    // 往 Token 的 payload (载荷) 里添加字段
                    context.getClaims().claim("userId", user.getId());
                }
            }
        };
    }
}
