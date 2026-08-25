package com.message.controller;

import com.message.service.impl.MessageDispatchServiceImpl;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * description WebRTC控制器
 * author zzq
 * date 2025/12/20 21:33
 */
@Slf4j
@Controller
public class RTCController {

    @Resource
    private MessageDispatchServiceImpl messageDispatchServiceImpl;

    // 定义一个简单的内部类用来接收信令数据 (你原来的 RTCResult)
    @Data
    public static class SignalMessage {
        private String type;      // "offer", "answer", "candidate"
        private String senderId;  // 由服务端根据已认证连接写入，不能信任前端传值
        private String targetId;  // 接收方 ID
        private Object data;      // SDP 或 Candidate 数据
    }

    /**
     * 1. 监考老师发起视频请求
     * 路径: /app/attemptVideo
     */
    @MessageMapping("/attemptVideo")
    public void attemptVideo(@Payload String targetStudentId, Principal principal) {
        if (!hasRole(principal, "teacher") && !hasRole(principal, "admin")) {
            log.warn("非监考人员尝试请求学生视频: {}", principal == null ? "anonymous" : principal.getName());
            return;
        }
        if (principal == null || targetStudentId == null || targetStudentId.isBlank()) {
            return;
        }
        String teacherId = principal.getName();
        log.info("👮‍ 监考老师 [{}] 请求查看学生 [{}] 的视频", teacherId, targetStudentId);
        // 给该学生发送指令：请初始化你的摄像头，并给我发 Offer
        // 消息发往: /user/{studentId}/queue/video-request
        messageDispatchServiceImpl.sendRawToUser(targetStudentId, "/queue/video-request", teacherId);
    }

    /**
     * 2. WebRTC 信令交换中转站
     * 路径: /app/webrtc/signal
     * 作用: 只要是 WebRTC 的数据，都通过这里转发
     */
    @MessageMapping("/webrtc/signal")
    public void forwardSignal(@Payload SignalMessage message, Principal principal) {
        if (principal == null || message == null || message.getTargetId() == null
                || message.getTargetId().isBlank()
                || message.getType() == null || message.getType().isBlank()) {
            return;
        }
        String senderId = principal.getName();
        String targetId = message.getTargetId();

        log.info("📡 转发信令 [{}] : 从 [{}] -> [{}]", message.getType(), senderId, targetId);

        // senderId 必须由已认证连接写入，防止前端伪造信令来源。
        message.setSenderId(senderId);
        // 将信令原文转发给目标用户
        // 目标用户订阅: /user/queue/webrtc/signal
        messageDispatchServiceImpl.sendRawToUser(targetId, "/queue/webrtc/signal", message);
    }

    private boolean hasRole(Principal principal, String expected) {
        if (!(principal instanceof Authentication authentication)) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(item -> item.replaceFirst("^ROLE_", ""))
                .anyMatch(item -> expected.equalsIgnoreCase(item));
    }
}
