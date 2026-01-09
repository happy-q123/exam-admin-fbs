package com.message.rmq.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

// 监听第二个 Topic
@Component
@Slf4j
@RocketMQMessageListener(topic = "test-delay", consumerGroup = "group-b")
public class ConsumerB implements RocketMQListener<String> {
    @Override
    public void onMessage(String message) {
        log.info("消费者收到消息: {}", message);
    }
}