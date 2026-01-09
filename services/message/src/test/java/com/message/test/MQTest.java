package com.message.test;

import com.rmq.producer.RMQProducerService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class MQTest {
    @Resource
    RMQProducerService rmqProducerService;

    @Test
    void testMessageSend() throws InterruptedException {

        rmqProducerService.sendTimerMessage("test-topic","延时消息",10000L);
    }

}
