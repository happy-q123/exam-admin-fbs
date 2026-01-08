package com.message.feign.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * description 全局feign拦截器，用于在“发出请求之前”，将security中的token传递给服务端。
 *      必须保证本服务收到的请求通过springsecurity过滤器链，这样才能有上下文。permitAll()方法不会通过。
 *      注册为bean，表示全局生效。
 *      若想作用域某feign客户端，则需要取消该类的bean，
 *      并使“作用域客户端”的注解变为@FeignClient(name = "服务名", configuration = FeignSecurityContextInterceptor.class)
 * author zzq
 * date 2025/12/20 17:34
 */
@Component
public class FeignSecurityContextInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return;
        }

        // 1. 处理标准的 HTTP 请求 (Spring Security OAuth2 自动生成的)
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            String tokenValue = jwtToken.getToken().getTokenValue();
            template.header("Authorization", "Bearer " + tokenValue);
        }
        // 2. 处理 WebSocket 上下文 (你在 WebSocketChannelInterceptor 中手动创建的)
        else if (authentication instanceof UsernamePasswordAuthenticationToken authToken) {
            // 你在 Interceptor 里把 raw token 放在了 credentials (第二个参数) 里
            Object credentials = authToken.getCredentials();
            if (credentials != null) {
                template.header("Authorization", "Bearer " + credentials.toString());
            }
        }
    }
}