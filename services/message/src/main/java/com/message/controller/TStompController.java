package com.message.controller;

import com.domain.dto.StompMessageDto;
import com.domain.restful.RestResponse;
import com.message.service.MessageDispatchService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.security.Principal;

/**
 * description TODO 责任链模式检查用户是否被禁用，是否在线
 * author zzq
 * date 2026/1/7 13:29
 * param * @param null
 * return
 */
@Controller
public class TStompController {

    private final MessageDispatchService messageDispatchService;

    public TStompController(MessageDispatchService messageDispatchService) {
        this.messageDispatchService = messageDispatchService;
    }

    @MessageMapping("/sayHello")
    public void sayHello(String message, Principal principal) {
        UsernamePasswordAuthenticationToken token=(UsernamePasswordAuthenticationToken) principal;
////        获取sub、scope等
//        @SuppressWarnings("unchecked")
//        Map<String, Object> details = (Map<String, Object>) token.getDetails();
        long userId= Long.parseLong(token.getName());
        messageDispatchService.sendToUser(Long.toString(userId),"/queue/sayHello",message);
        System.out.println("Received message: " + message);
    }

    @PostMapping("/infoOnlineUsers")
    public RestResponse infoOnlineUsers(@RequestBody StompMessageDto tdo) {
        messageDispatchService.sendToUser(tdo.getReceiverId(),tdo.getDestination(),tdo.getMessage());
        return RestResponse.success();
    }

}
