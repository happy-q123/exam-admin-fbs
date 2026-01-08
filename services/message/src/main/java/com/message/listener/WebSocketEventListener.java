package com.message.listener;

import com.domain.dto.UserOnlineExamOptionsDto;
import com.domain.enums.UserOnlineExamOptionTypeEnum;
import com.domain.enums.redis.OnlineExamEnum;
import com.domain.enums.redis.UserOnlineKeyEnum;
import com.message.feign.OnlineExamFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.LocalDateTime;

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

    @Autowired
    OnlineExamFeignClient onlineExamFeignClient;

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
            // 从 Redis的在线用户列表 移除
            redisTemplate.opsForSet().remove(User_Online_Key, userId);

            // 退出时查询用户是否正在参与考试，如果在，则向考试写入“考试退出”记录。
            processUserDropOnline(userId);
        }
    }

    /**
     * description
     * author zzq
     * date 2026/1/8 15:54
     */
    private void processUserDropOnline(String userId) {
        String key=OnlineExamEnum.Is_Examing.buildKey(userId);

        String examIdV=redisTemplate.opsForValue().get(key);
        if (examIdV==null){
            return;
        }
        Long examId=Long.valueOf(examIdV);
        UserOnlineExamOptionsDto dto=UserOnlineExamOptionsDto.builder()
                .userId(Long.valueOf(userId))
                .examId(examId)
                .optionType(UserOnlineExamOptionTypeEnum.Exit)
                .optionTime(LocalDateTime.now()).build();

        onlineExamFeignClient.exitOnlineExam(dto);
    }
}