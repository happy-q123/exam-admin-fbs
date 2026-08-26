package com.exam.feign;

import com.domain.dto.StompMessageDto;
import com.domain.restful.RestResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "message-service")
public interface InfoOnlineUserFeignClient {
    @PostMapping("/infoOnlineUsers")
    RestResponse<Void> infoOnlineUsers(@RequestBody StompMessageDto stompMessageDto);
}
