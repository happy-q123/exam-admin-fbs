package com.rmq.producer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.DelayMode;
import org.springframework.cglib.core.Local;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

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
     * description 送任意时长的延时消息 (RocketMQ 5.x 原生支持)
     * author zzq
     * date 2026/1/9 18:08
     */
    public void sendTimerMessage(String topic, String msgContent, long timeSize, DelayMode timeUnit) {
        // 记录当前时间用于计算相对时间的目标点
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime targetTime = null;

        if (DelayMode.DELAY_MILLISECONDS.equals(timeUnit)) {
            // 相对毫秒延迟
            // 调用源码中定义的 syncSendDelayTimeMills
            rocketMQTemplate.syncSendDelayTimeMills(topic, msgContent, timeSize);
            // 计算预估投递时间：当前时间 + 毫秒数
            targetTime = currentTime.plus(timeSize, ChronoUnit.MILLIS);

        } else if (DelayMode.DELAY_SECONDS.equals(timeUnit)) {
            // 相对秒级延迟
            // 调用源码中定义的 syncSendDelayTimeSeconds
            rocketMQTemplate.syncSendDelayTimeSeconds(topic, msgContent, timeSize);
            // 计算预估投递时间：当前时间 + 秒数
            targetTime = currentTime.plusSeconds(timeSize);

        } else if (DelayMode.DELIVER_TIME_MILLISECONDS.equals(timeUnit)) {
            // 绝对时间投递 (指定时间戳)
            // 注意：这里的 timeSize 实际上代表的是 绝对时间戳 (System.currentTimeMillis() + delay)
            // 调用源码中定义的 syncSendDeliverTimeMills
            rocketMQTemplate.syncSendDeliverTimeMills(topic, msgContent, timeSize);

            // 将时间戳转换为 LocalDateTime 用于日志打印
            targetTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timeSize), ZoneId.systemDefault());
        }

        log.info("定时消息已发送 | 模式: {} | 数值: {} | 预计投递时间: {}", timeUnit, timeSize, targetTime);
    }

    /*
    *
        // 场景1：延迟 500 毫秒 (相对)
    sendTimerMessage("test-topic", "毫秒延迟", 500L, DelayMode.DELAY_MILLISECONDS);

    // 场景2：延迟 10 秒 (相对)
    sendTimerMessage("test-topic", "秒延迟", 10L, DelayMode.DELAY_SECONDS);

    // 场景3：定在明天早上 8:00 投递 (绝对)
    long tomorrow8am = LocalDateTime.now()
            .plusDays(1)
            .withHour(8).withMinute(0).withSecond(0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

    // 注意：这里传进去的是计算好的“未来时间戳”
    sendTimerMessage("test-topic", "绝对时间", tomorrow8am, DelayMode.DELIVER_TIME_MILLISECONDS);
    *
    * */
}