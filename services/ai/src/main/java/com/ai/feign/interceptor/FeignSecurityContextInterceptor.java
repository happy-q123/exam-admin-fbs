package com.ai.feign.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
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
 * date 2026/1/28 19:50
 */
@Component
public class FeignSecurityContextInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 检查是否是 JWT 类型
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            // 获取原始 Token 字符串
            String tokenValue = jwtToken.getToken().getTokenValue();
            // 手动拼接 Bearer
            template.header("Authorization", "Bearer " + tokenValue);
        }
    }
}