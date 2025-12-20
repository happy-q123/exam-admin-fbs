package com.message.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 开启注解权限控制 (如 @PreAuthorize)
public class SecurityConfig {

    /**
     * 配置过滤器链
     * 核心作用：决定哪些接口公开，哪些需要登录
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. 禁用 CSRF
                // 因为我们要暴露 REST API 给 Auth Server 调用，不需要防跨站攻击保护
                // 如果不禁用，POST 请求会被拦截
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)

                // 2. 配置权限规则
                .authorizeHttpRequests(auth -> auth
                        //开放ws握手地址
                        //ws的握手前期需要http，且浏览器的这个http握手不能携带header，所以不能让security处理token，只能配置ws拦截器
                        .requestMatchers("/ws","/ws-sockjs").permitAll()
                        .requestMatchers("/error").permitAll()

                        // 其他所有请求都需要认证（虽然 Auth Server 不会调别的，但这是安全兜底）
                        .anyRequest().authenticated()
                );
        // 3. 开启 OAuth2 Resource Server 功能，并使用 JWT 解析器
        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

}