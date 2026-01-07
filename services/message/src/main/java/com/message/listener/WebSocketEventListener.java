package com.message.listener;

import com.domain.enums.redis.UserOnlineKeyEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * description WebSocket 连接事件监听,目前仅记录用户上线和下线
 * author zzq
 * date 2025/12/20 21:10
 */
@Slf4j
@Component
public class WebSocketEventListener {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String User_Online_Key = UserOnlineKeyEnum.ONLINE_USERS.buildKey();
    /**
     * 监听：连接成功
     */
    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        
        if (user != null) {
            String userId = user.getName();
            log.info("✅ 用户上线: {}", userId);
            // 存入 Redis Set 集合
            redisTemplate.opsForSet().add(User_Online_Key, userId);
        }
    }

    /**
     * 监听：连接断开
     */
    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();

        if (user != null) {
            String userId = user.getName();
            log.info("❌ 用户下线: {}", userId);
            // 从 Redis 移除
            redisTemplate.opsForSet().remove(User_Online_Key, userId);
        }
    }
}