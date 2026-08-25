package com.exam.websocket;

import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.socket.server.standard.SpringConfigurator;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@ServerEndpoint(value = "/ws/proctor/{examId}/{role}/{userId}", configurator = SpringConfigurator.class)
public class ProctorWebSocketServer {

    // 存储某个考试的监考老师的 Session (考场ID -> 老师们的会话集合)
    private static final ConcurrentHashMap<String, CopyOnWriteArraySet<Session>> teacherSessions = new ConcurrentHashMap<>();
    private static volatile JwtDecoder jwtDecoder;

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
            boolean observer = "teacher".equalsIgnoreCase(role)
                    && ("teacher".equalsIgnoreCase(tokenRole) || "admin".equalsIgnoreCase(tokenRole));
            boolean student = "student".equalsIgnoreCase(role) && "student".equalsIgnoreCase(tokenRole);
            if (!userId.equals(tokenUserId) || (!observer && !student)) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "监考身份与令牌不匹配"));
                return;
            }
        } catch (JwtException ex) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "访问令牌无效"));
            return;
        }
        if ("teacher".equalsIgnoreCase(role)) {
            teacherSessions.computeIfAbsent(examId, k -> new CopyOnWriteArraySet<>()).add(session);
            System.out.println("监考老师上线: " + userId + ", 考场: " + examId);
        } else if ("student".equalsIgnoreCase(role)) {
            System.out.println("学生上线准备推送画面: " + userId + ", 考场: " + examId);
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
                            tSession.getBasicRemote().sendText(forwardMsg);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }
}
