package com.rmq.enums;

import com.rmq.consts.TopicConst;
import lombok.Getter;

/**
 * description RMQ的topic枚举
 * author zzq
 * date 2026/1/10 17:07
 */
public enum RMQTopicEnum {

    DEFAULT(TopicConst.DEFAULT_TOPIC);

    @Getter
    private final String topic;

    RMQTopicEnum(String topic) {
        this.topic = topic;
    }

    @Override
    public String toString() {
        return this.topic;
    }
}

