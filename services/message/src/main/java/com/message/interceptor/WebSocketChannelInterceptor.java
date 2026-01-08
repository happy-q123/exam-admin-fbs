package com.message.interceptor;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

/**
 * description ws的握手前期需要http，且浏览器的这个握手http不能携带header，所以不能让security处理token，只能配置ws拦截器
 * STOMP 连接 (CONNECT 帧)：连接建立后的几毫秒内，前端发送 STOMP 的 CONNECT 帧，这里面可以带 Header，因此在这里拦截，处理token。
 * author zzq
 * date 2025/12/20 20:53
 */
@Slf4j
@Component
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    // 注入官方的 JwtDecoder (Spring Boot OAuth2 自动配置好的)
    @Resource
    private JwtDecoder jwtDecoder;

    // 可选：如果你自定义了权限转换器，也可以注入进来
    // private JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // 只拦截 CONNECT 帧
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

            // 获取 Authorization 头 (前端应该传 "Bearer eyJhbGciO...")
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            log.info("WebSocket 连接请求，Authorization: {}", authHeader);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7); // 去掉 "Bearer " 前缀

                try {
                    Jwt jwt = jwtDecoder.decode(token);
                    // 1. 从 JWT Claims 中获取 userId (假设你的 token 里存的是 "userId" 或 "id")
                    // 注意：根据你的生成逻辑，这里可能是 String 也可能是 Long
                    String userIdObj = jwt.getClaims().get("userId").toString();
                    Principal authentication = getPrincipal(userIdObj, jwt, token);

                    // 5. 绑定到 WebSocket Session
                    accessor.setUser(authentication);

                    log.info("✅ OAuth2 认证成功，用户ID: {}", authentication.getName()); // 现在这里打印的就是 ID 了

                } catch (JwtValidationException e) {
                    log.error("❌ Token 验证失败: {}", e.getMessage());
                    // 也可以选择在这里抛出异常，强制断开连接
                } catch (Exception e) {
                    log.error("❌ WebSocket 认证过程出错", e);
                }
            } else {
                log.warn("⚠️ 未携带 Bearer Token");
            }
        }
        return message;
    }

    private UsernamePasswordAuthenticationToken getPrincipal(Object userIdObj, Jwt jwt, String token) {
        String userIdString = String.valueOf(userIdObj);

        // 建议先给空权限，跑通了再说。或者手动构建 SimpleGrantedAuthority
        List<GrantedAuthority> authorities = Collections.emptyList();

        // 或者如果你想把 sub 当权限（虽然不常见）：
         if (jwt.getSubject() != null) {
            authorities = List.of(new SimpleGrantedAuthority("ROLE_" + jwt.getSubject()));
         }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userIdString, token, authorities);

        authentication.setDetails(jwt.getClaims());
        return authentication;
    }
}