package com.message.interceptor;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

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
                    // 使用官方 Decoder 验证 Token
                    // 如果 Token 过期或无效，这里会直接抛出 JwtValidationException
                    Jwt jwt = jwtDecoder.decode(token);

                    // 将 JWT 转换为 Spring Security 的认证对象
                    // JwtAuthenticationToken 是 OAuth2 Resource Server 标准的 Principal
                    JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);

                    // 如果你需要解析角色（Authorities），可以用 Converter，默认只会解析 scope
//                     JwtAuthenticationToken authentication = (JwtAuthenticationToken) jwtAuthenticationConverter.convert(jwt);

                    // 绑定到 WebSocket Session
                    accessor.setUser(authentication);

                    log.info("✅ OAuth2 认证成功，用户: {}", authentication.getName()); // 默认是 sub 字段

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
}