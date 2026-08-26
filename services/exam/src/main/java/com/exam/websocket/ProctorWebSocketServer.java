package com.exam.websocket;

import com.exam.service.ProctorEventService;
import com.exam.service.ExamService;
import com.exam.service.UserApplyExamRelationService;
import com.domain.entity.Exam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
@ServerEndpoint(value = "/ws/proctor/{examId}/{role}/{userId}", configurator = SpringEndpointConfigurator.class)
public class ProctorWebSocketServer {

    // 存储某个考试的监考老师的 Session (考场ID -> 老师们的会话集合)
    private static final ConcurrentHashMap<String, CopyOnWriteArraySet<Session>> teacherSessions = new ConcurrentHashMap<>();
    private static volatile JwtDecoder jwtDecoder;
    @Autowired
    private ProctorEventService proctorEventService;
    @Autowired
    private ExamService examService;
    @Autowired
    private UserApplyExamRelationService userApplyExamRelationService;

    public ProctorWebSocketServer(JwtDecoder jwtDecoder) {
        ProctorWebSocketServer.jwtDecoder = jwtDecoder;
    }

    @OnOpen
    public void onOpen(Session session,
                       @PathParam("examId") String examId,
                       @PathParam("role") String role,
                       @PathParam("userId") String userId) throws IOException {
        if (!"teacher".equalsIgnoreCase(role) && !"student".equalsIgnoreCase(role)) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "非法监考角色"));
            return;
        }
        java.util.List<String> tokens = session.getRequestParameterMap().get("token");
        if (tokens == null || tokens.isEmpty() || tokens.get(0).isBlank()) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "缺少访问令牌"));
            return;
        }
        if (jwtDecoder == null) {
            session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "监考服务未完成鉴权配置"));
            return;
        }
        try {
            Jwt jwt = jwtDecoder.decode(tokens.get(0));
            String tokenUserId = jwt.getClaimAsString("userId");
            String tokenRole = jwt.getClaimAsString("role");
            Object tokenRoles = jwt.getClaim("roles");
            Object tokenAuthorities = jwt.getClaim("authorities");
            boolean observer = "teacher".equalsIgnoreCase(role)
                    && (hasRole(tokenRole, tokenRoles, tokenAuthorities, "teacher")
                    || hasRole(tokenRole, tokenRoles, tokenAuthorities, "admin"));
            boolean student = "student".equalsIgnoreCase(role)
                    && hasRole(tokenRole, tokenRoles, tokenAuthorities, "student");
            Exam exam = examService == null ? null : examService.getById(Long.valueOf(examId));
            boolean examOwner = exam != null && tokenUserId != null
                    && (hasRole(tokenRole, tokenRoles, tokenAuthorities, "admin")
                    || (hasRole(tokenRole, tokenRoles, tokenAuthorities, "teacher")
                    && Long.valueOf(tokenUserId).equals(exam.getCreator())));
            boolean enrolled = exam != null && Boolean.TRUE.equals(exam.getStatus())
                    && userApplyExamRelationService != null
                    && userApplyExamRelationService.checkExamApplyExist(Long.valueOf(tokenUserId), Long.valueOf(examId));
            if (!userId.equals(tokenUserId) || (!observer && !student)
                    || (observer && !examOwner) || (student && !enrolled)) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "监考身份与令牌不匹配"));
                return;
            }
        } catch (RuntimeException ex) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "访问令牌无效"));
            return;
        }
        if ("teacher".equalsIgnoreCase(role)) {
            teacherSessions.computeIfAbsent(examId, k -> new CopyOnWriteArraySet<>()).add(session);
            log.info("监考老师上线: userId={}, examId={}", userId, examId);
        } else if ("student".equalsIgnoreCase(role)) {
            recordEvent(examId, userId, "ONLINE", Map.of("channel", "proctor-websocket"));
            log.info("学生上线准备推送画面: userId={}, examId={}", userId, examId);
        }
    }

    @OnClose
    public void onClose(Session session,
                        @PathParam("examId") String examId,
                        @PathParam("role") String role,
                        @PathParam("userId") String userId) {
        if ("teacher".equalsIgnoreCase(role)) {
            CopyOnWriteArraySet<Session> sessions = teacherSessions.get(examId);
            if (sessions != null) {
                sessions.remove(session);
            }
        } else if ("student".equalsIgnoreCase(role)) {
            recordEvent(examId, userId, "OFFLINE", Map.of("channel", "proctor-websocket"));
        }
    }

    @OnMessage
    public void onMessage(String message, Session session,
                          @PathParam("examId") String examId,
                          @PathParam("role") String role,
                          @PathParam("userId") String userId) {
        // 只有学生发来的视频帧才需要被处理
        if ("student".equalsIgnoreCase(role)) {
            // 将学生发来的 Base64 加上 userId 的前缀，转发给该考场所有在线的老师
            // 格式： userId:Base64Data
            String forwardMsg = userId + ":" + message;
            CopyOnWriteArraySet<Session> teachers = teacherSessions.get(examId);
            if (teachers != null && !teachers.isEmpty()) {
                for (Session tSession : teachers) {
                    if (tSession.isOpen()) {
                        try {
                            tSession.getAsyncRemote().sendText(forwardMsg);
                        } catch (Exception e) {
                            log.error("转发监考视频帧异常: {}", e.getMessage());
                        }
                    }
                }
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("监考 WebSocket 发生异常", error);
    }

    private boolean hasRole(String role, Object roles, Object authorities, String expected) {
        if (expected.equalsIgnoreCase(role)) return true;
        return hasRoleInClaim(roles, expected) || hasRoleInClaim(authorities, expected);
    }

    private boolean hasRoleInClaim(Object claim, String expected) {
        if (!(claim instanceof java.util.Collection<?> collection)) return false;
        return collection.stream().map(String::valueOf)
                .map(item -> item.replaceFirst("^ROLE_", ""))
                .anyMatch(item -> expected.equalsIgnoreCase(item));
    }

    private void recordEvent(String examId, String userId, String eventType, Map<String, Object> payload) {
        try {
            if (proctorEventService != null) {
                proctorEventService.record(Long.valueOf(examId), Long.valueOf(userId), eventType, payload);
            }
        } catch (RuntimeException ignored) {
            // 监考画面通道不能因事件审计表短暂不可用而断开。
        }
    }
}
