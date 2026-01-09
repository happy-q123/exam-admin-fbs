package com.message.service.impl;

import com.domain.restful.RestResponse; // 替换引用
import com.message.service.MessageDispatchService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.messaging.support.MessageBuilder;
import java.util.Collection;
import java.util.Map;

/**
 * description websocket消息发送服务
 * author zzq
 * date 2025/12/20 21:34
 */
@Slf4j
@Service
public class MessageDispatchServiceImpl implements MessageDispatchService {

    @Resource
    private SimpMessagingTemplate messagingTemplate;

    @Value("${socket.message.dispatch.log:false}")
    private boolean useLog;

    /**
     * 发送给单人 (最常用)
     */
    @Override
    public void sendToUser(String userId, String destination, Object payload) {
        // 自动包裹一层 RestResponse，保证前端收到的格式永远统一
        RestResponse<Object> response = RestResponse.success(payload);
        doSendToUser(userId, destination, response, null);
    }

    /**
     * 批量发送给多人
     */
    @Override
    public void sendToUsers(Collection<String> userIds, String destination, Object payload) {
        RestResponse<Object> response = RestResponse.success(payload);
        userIds.forEach(userId -> doSendToUser(userId, destination, response, null));
    }

    /**
     * 发送带 Header 的消息 (高级功能：比如需要传 token 或 timestamp)
     */
    public void sendToUserWithHeaders(String userId, String destination, Object payload, Map<String, Object> headers) {
        RestResponse<Object> response = RestResponse.success(payload);
        doSendToUser(userId, destination, response, headers);
    }

    //广播
    @Override
    public void sendToAll(String destination, Object payload) {
        RestResponse<Object> response = RestResponse.success(payload);
        try {
            messagingTemplate.convertAndSend(destination, response);
            if (useLog){
                log.info("📢 广播消息 -> 路径: {}, 内容摘要: {}", destination, getLogSummary(payload));
            }
        } catch (MessagingException e) {
            log.error("❌ 广播失败: {}", e.getMessage());
        }
    }

    /**
     * 真正的发送逻辑封装
     */
    private void doSendToUser(String userId, String destination, Object finalPayload, Map<String, Object> headers) {
        try {
            if (headers != null && !headers.isEmpty()) {
                // 使用 MessageBuilder 重建消息
                messagingTemplate.convertAndSendToUser(userId, destination, finalPayload, message -> {
                    // 基于原消息创建一个 Builder (这样保留了原消息的 ID、Payload 等信息)
                    MessageBuilder<?> builder = MessageBuilder.fromMessage(message);

                    //循环设置 Header
                    headers.forEach(builder::setHeader);

                    //build() 生成一个新的 Message 对象返回
                    return builder.build();
                });
            } else {
                // 标准发送
                messagingTemplate.convertAndSendToUser(userId, destination, finalPayload);
            }
            if(useLog){
                log.info("📧 私信 -> 用户: {}, 路径: {}, 内容摘要: {}", userId, destination, getLogSummary(finalPayload));
            }
        } catch (Exception e) {
            log.error("❌ 发送私信失败 -> 用户: {}, 原因: {}", userId, e.getMessage());
        }
    }

    /**
     * 防止大对象撑爆日志
     */
    private String getLogSummary(Object payload) {
        String str = String.valueOf(payload);
        return str.length() > 100 ? str.substring(0, 100) + "..." : str;
    }
}