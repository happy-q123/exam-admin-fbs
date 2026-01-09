package com.rmq.producer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * description rocket生产者（消息发送者）
 * author zzq
 * date 2026/1/9 15:33
 */
@Component
@Slf4j
public class RMQProducerService {
    private final RocketMQTemplate rocketMQTemplate;
    public RMQProducerService(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    // 发送普通同步消息
    public void sendSyncMessage(String topic, String msg) {
        // convertAndSend 是最简单的方法
        rocketMQTemplate.convertAndSend(topic, msg);
        log.info("同步消息发送成功");
    }

    // 发送带 Tag 的消息 (格式: topic:tag)
    public void sendMessageWithTag(String msg) {
        rocketMQTemplate.convertAndSend("test-topic:tagA", msg);
    }

    // 发送异步消息
    public void sendAsyncMessage(String topic, Object msg) {
        rocketMQTemplate.asyncSend(topic, msg, new org.apache.rocketmq.client.producer.SendCallback() {
            @Override
            public void onSuccess(org.apache.rocketmq.client.producer.SendResult sendResult) {
                log.info("异步发送成功: {}", sendResult.getMsgId());
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("异步发送失败: ", throwable);
            }
        });
    }
    
    // 发送 Spring Message 对象 (包含 Header 等信息)
    public void sendSpringMessage(String topic, String msgContent) {
        rocketMQTemplate.send(topic, MessageBuilder.withPayload(msgContent).build());
    }

    /**
     * 发送任意时长的延时消息 (RocketMQ 5.x 原生支持)
     * @param topic Topic
     * @param msgContent 消息内容
     * @param delayTimeMs 延时时长（毫秒），例如 60000L 代表 1分钟
     */
    public void sendTimerMessage(String topic, String msgContent, long delayTimeMs) {
        // 计算目标投递时间戳
        long targetTime = System.currentTimeMillis() + delayTimeMs;

        Message<String> message = MessageBuilder.withPayload(msgContent)
                // 核心：设置 __STARTDELIVERTIME 属性 (绝对时间戳)
                .setHeader("__STARTDELIVERTIME", String.valueOf(targetTime))
                .build();

        // 发送消息
        rocketMQTemplate.syncSend(topic, message);
        log.info("定时消息已发送，将在 {} 投递", targetTime);
    }
}