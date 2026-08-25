package com.message.interceptor;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
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
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.ArrayList;
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

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new MessageDeliveryException("WebSocket连接缺少Bearer Token");
            }

            String token = authHeader.substring(7).trim();
            if (token.isEmpty()) {
                throw new MessageDeliveryException("WebSocket连接令牌为空");
            }

            try {
                Jwt jwt = jwtDecoder.decode(token);
                String userId = jwt.getClaimAsString("userId");
                if (userId == null || userId.isBlank()) {
                    throw new MessageDeliveryException("Token中缺少userId");
                }
                Principal authentication = getPrincipal(userId, jwt);
                accessor.setUser(authentication);
                log.info("✅ OAuth2 WebSocket认证成功，用户ID: {}", authentication.getName());
            } catch (JwtException e) {
                log.warn("❌ WebSocket Token验证失败: {}", e.getMessage());
                throw new MessageDeliveryException("WebSocket Token无效: " + e.getMessage());
            }
        }
        return message;
    }

    private UsernamePasswordAuthenticationToken getPrincipal(String userIdString, Jwt jwt) {

        List<GrantedAuthority> authorities = new ArrayList<>();
        Object claimAuthorities = jwt.getClaims().get("authorities");
        if (claimAuthorities instanceof List<?> values) {
            values.forEach(value -> addAuthority(authorities, value));
        }
        String role = jwt.getClaimAsString("role");
        addAuthority(authorities, role);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userIdString, null, authorities);

        authentication.setDetails(jwt.getClaims());
        return authentication;
    }

    private void addAuthority(List<GrantedAuthority> authorities, Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return;
        }
        String normalized = String.valueOf(value).replaceFirst("^ROLE_", "");
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + normalized);
        if (!authorities.contains(authority)) {
            authorities.add(authority);
        }
    }
}
