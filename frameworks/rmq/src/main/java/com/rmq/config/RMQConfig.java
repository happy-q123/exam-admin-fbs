package com.rmq.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * description 这样才能让其它服务通过pom的方式找到
 * author zzq
 * date 2026/1/9 15:59
 */
@Configuration
@ComponentScan("com.rmq.producer") // 确保能扫描到 RMQProducerService
@ConditionalOnProperty(prefix = "exam.messaging", name = "rocketmq-enabled", havingValue = "true")
public class RMQConfig {
}
