package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domain.entity.ProctorEvent;

import java.util.Map;

public interface ProctorEventService extends IService<ProctorEvent> {
    void record(Long examId, Long studentId, String eventType, Map<String, Object> payload);
}
