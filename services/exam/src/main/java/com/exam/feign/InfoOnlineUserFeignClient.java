package com.exam.feign;

import com.domain.dto.StompMessageDto;
import com.domain.restful.RestResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "message-service")
public interface InfoOnlineUserFeignClient {
    @GetMapping("/infoOnlineUsers")
    RestResponse infoOnlineUsers(@RequestBody StompMessageDto stompMessageDto);
}
