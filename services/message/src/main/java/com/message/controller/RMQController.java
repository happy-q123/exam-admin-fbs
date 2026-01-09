package com.message.controller;

import com.domain.restful.RestResponse;
import com.rmq.producer.RMQProducerService;
import org.apache.rocketmq.spring.support.DelayMode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RMQController {
    private final RMQProducerService rmqProducerService;

    public RMQController(RMQProducerService rmqProducerService) {
        this.rmqProducerService = rmqProducerService;
    }

    @GetMapping("/testDelayMessage")
    RestResponse testDelayMessage(){
        rmqProducerService.sendTimerMessage("test-delay","测试延时消息",100000L, DelayMode.DELAY_MILLISECONDS);
        return RestResponse.success();
    }
}
