package com.message.test;

import com.rmq.producer.RMQProducerService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
@ActiveProfiles("mq")
public class MQTest {
    @Resource
    RMQProducerService rmqProducerService;

    @Test
    void testMessageSend() throws InterruptedException {

        rmqProducerService.sendTimerMessage("test-topic","延时消息",10000L);
    }

}
