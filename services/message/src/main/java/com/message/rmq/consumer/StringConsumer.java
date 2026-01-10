package com.message.rmq.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 简单的字符串消息消费者
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = "test-topic",          // 监听的 Topic
    consumerGroup = "my-consumer-group", // 消费者组
    selectorExpression = "*"       // Tag 过滤，* 表示接收所有 tag，或指定 "tagA || tagB"
)
public class StringConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        log.info("消费者收到消息: {}", message);
    }
}