package com.message.controller;

import com.domain.dto.StompMessageDto;
import com.domain.restful.RestResponse;
import com.message.service.MessageDispatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;

/**
 * description TODO 责任链模式检查用户是否被禁用，是否在线
 * author zzq
 * date 2026/1/7 13:29
 * param * @param null
 * return
 */
@Controller
@Slf4j
public class TStompController {

    private static final int MAX_DESTINATION_LENGTH = 128;
    private static final int MAX_MESSAGE_LENGTH = 64 * 1024;

    private final MessageDispatchService messageDispatchService;

    public TStompController(MessageDispatchService messageDispatchService) {
        this.messageDispatchService = messageDispatchService;
    }

    @MessageMapping("/sayHello")
    public void sayHello(String message, Principal principal) {
        String userId = authenticatedUserId(principal);
        if (userId == null) {
            return;
        }
        messageDispatchService.sendToUser(userId, "/queue/sayHello", message);
        log.debug("STOMP sayHello 已发送: userId={}", userId);
    }

    @PostMapping("/infoOnlineUsers")
    @ResponseBody
    @PreAuthorize("@roleGuard.isTeacherOrAdmin(authentication)")
    public RestResponse<Void> infoOnlineUsers(@RequestBody StompMessageDto dto) {
        validateRelayMessage(dto);
        messageDispatchService.sendToUser(dto.getReceiverId(), dto.getDestination(), dto.getMessage());
        return RestResponse.success();
    }

    private String authenticatedUserId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            log.warn("拒绝没有有效身份的 STOMP 消息");
            return null;
        }
        try {
            long userId = Long.parseLong(principal.getName());
            if (userId <= 0) {
                throw new NumberFormatException("user id must be positive");
            }
            return Long.toString(userId);
        } catch (NumberFormatException ex) {
            log.warn("拒绝无效用户身份的 STOMP 消息: principal={}", principal.getName());
            return null;
        }
    }

    private void validateRelayMessage(StompMessageDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("消息不能为空");
        }
        if (dto.getReceiverId() == null || dto.getReceiverId().isBlank()) {
            throw new IllegalArgumentException("接收用户不能为空");
        }
        try {
            if (Long.parseLong(dto.getReceiverId()) <= 0) {
                throw new NumberFormatException("receiver id must be positive");
            }
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("接收用户格式不正确");
        }
        if (dto.getDestination() == null || dto.getDestination().isBlank()
                || !dto.getDestination().startsWith("/queue/")
                || dto.getDestination().length() > MAX_DESTINATION_LENGTH
                || dto.getDestination().contains("..")
                || dto.getDestination().chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("消息目的地不合法");
        }
        if (dto.getMessage() != null && dto.getMessage().length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("消息内容过大");
        }
    }

}
