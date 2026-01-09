package com.message.service.schedule;

import com.message.service.MessageDispatchService;
import com.rmq.producer.RMQProducerService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
/**
 * description 广播系统时间
 * author zzq
 * date 2026/1/9 14:33
 */
@Service
public class TimeUnifyService {

    private final RMQProducerService rmqProducerService;
    private final MessageDispatchService messageDispatchService;

    public TimeUnifyService(RMQProducerService rmqProducerService, MessageDispatchService messageDispatchService) {
        this.rmqProducerService = rmqProducerService;
        this.messageDispatchService = messageDispatchService;
    }

    //每30秒同步一下时间
    @Scheduled(cron = "*/30 * * * * *")
//    @Scheduled(cron = "0,30 * * * * *")//这样写也可以
    public void broadServerTime() {
        LocalDateTime now=LocalDateTime.now();
        messageDispatchService.sendToAll("/topic/unifyTime",now);
    }
}
