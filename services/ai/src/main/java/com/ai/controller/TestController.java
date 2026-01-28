package com.ai.controller;

import com.ai.feign.UserErrorQuestionFeignClient;
import com.domain.restful.RestResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    private final UserErrorQuestionFeignClient userErrorQuestionFeignClient;

    public TestController(UserErrorQuestionFeignClient userErrorQuestionFeignClient) {
        this.userErrorQuestionFeignClient = userErrorQuestionFeignClient;
    }

    @GetMapping("/test")
    public String test(){
        return "test";
    }

    @GetMapping("/testFeignClient")
    public RestResponse testFeignClient(){
        Object o=userErrorQuestionFeignClient.getErrorQuestionsByUserId(1111L);
        return RestResponse.success(o);
    }
}
