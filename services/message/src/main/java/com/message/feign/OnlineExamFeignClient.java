package com.message.feign;

import com.domain.dto.UserOnlineExamOptionsDto;
import com.domain.restful.RestResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "question-service")
public interface OnlineExamFeignClient {

    @PostMapping("/exam/exitOnlineExam")
    RestResponse<String> exitOnlineExam(@RequestBody UserOnlineExamOptionsDto dto);
}