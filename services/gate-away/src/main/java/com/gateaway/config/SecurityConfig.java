package com.gateaway.config;//package com.gateaway.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.Customizer;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
//import org.springframework.security.config.web.server.ServerHttpSecurity;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.server.SecurityWebFilterChain;
//
//@Configuration
//@EnableWebFluxSecurity
//public class SecurityConfig {
//
//    @Bean
//    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
//
//        http
//                // Gateway 不需要 CSRF
//                .csrf(ServerHttpSecurity.CsrfSpec::disable)
//                // 只做“是否已认证”的判断
//                .authorizeExchange(exchange -> exchange
//                        .pathMatchers("/auth/**").permitAll()
//                        .pathMatchers("/user/common/**","/user/common/register").permitAll()
//                        .pathMatchers("/message/ws","/message/ws-sockjs").permitAll()
//                        .pathMatchers("/error").permitAll()
//                        .anyExchange().authenticated()
//                )
//                // 开启 OAuth2 Resource Server（JWT）
//                .oauth2ResourceServer(oauth2 ->
//                        oauth2.jwt(jwt -> {})
//                );
//
//        return http.build();
//    }
//}

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // 1. 这里的 authorizeExchange 保持不变
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/auth/**", "/user/common/**", "/error").permitAll()
                        .pathMatchers(org.springframework.http.HttpMethod.OPTIONS).permitAll() // 放行预检
                        .anyExchange().authenticated()
                )
                // 2. 【核心修改】把异常处理放到最外层的 exceptionHandling 中
                // 这样无论是 Token 错误，还是没传 Token 导致的 401，都能捕获！
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(customAuthenticationEntryPoint()) // 处理 401
                        .accessDeniedHandler(customAccessDeniedHandler())           // 处理 403
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                );

        return http.build();
    }

    // 自定义 401 响应
    private ServerAuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (exchange, ex) -> {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            // 打印一下日志，方便看控制台
            System.out.println("触发 401 异常: " + ex.getMessage());
            String body = "{\"code\": 401, \"msg\": \"未授权或Token无效: " + ex.getMessage() + "\"}";
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        };
    }

    // 自定义 403 响应
    private ServerAccessDeniedHandler customAccessDeniedHandler() {
        return (exchange, ex) -> {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.FORBIDDEN);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            String body = "{\"code\": 403, \"msg\": \"权限不足: " + ex.getMessage() + "\"}";
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        };
    }
}