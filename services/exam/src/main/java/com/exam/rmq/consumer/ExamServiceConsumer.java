package com.exam.rmq.consumer;

import com.domain.dto.StompMessageDto;
import com.exam.feign.InfoOnlineUserFeignClient;
import com.rmq.consts.ConsumerGroupConst;
import com.rmq.consts.TopicConst;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = TopicConst.DEFAULT_TOPIC,          // 监听的 Topic
        consumerGroup = ConsumerGroupConst.DEFAULT_CONSUMER_GROUP, // 消费者组
        selectorExpression = "*"       // Tag 过滤，* 表示接收所有 tag，或指定 "tagA || tagB"
)
public class ExamServiceConsumer implements RocketMQListener<String> {
    private final InfoOnlineUserFeignClient infoOnlineUserFeignClient;

    public ExamServiceConsumer(InfoOnlineUserFeignClient infoOnlineUserFeignClient) {
        this.infoOnlineUserFeignClient = infoOnlineUserFeignClient;
    }

    //todo 写一个rmq消息传输体
    @Override
    public void onMessage(String s) {
        String examId=s;
        StompMessageDto dto = StompMessageDto.builder()
                .message("")
                .destination("/queue/close-exam")
                .receiverId(s)
                .build();

//        infoOnlineUserFeignClient.infoOnlineUsers(dto);
        log.info("应该统一关闭考试了: {}", s);

    }
}
