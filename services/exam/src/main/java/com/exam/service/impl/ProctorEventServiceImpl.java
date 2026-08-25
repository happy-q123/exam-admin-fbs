package com.exam.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.entity.ProctorEvent;
import com.exam.mapper.ProctorEventMapper;
import com.exam.service.ProctorEventService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ProctorEventServiceImpl extends ServiceImpl<ProctorEventMapper, ProctorEvent>
        implements ProctorEventService {
    @Override
    public void record(Long examId, Long studentId, String eventType, Map<String, Object> payload) {
        if (examId == null || studentId == null || eventType == null || eventType.isBlank()) return;
        ProctorEvent event = new ProctorEvent();
        event.setExamId(examId);
        event.setStudentId(studentId);
        event.setEventType(eventType);
        event.setPayload(payload == null ? Map.of() : payload);
        event.setCreatedTime(LocalDateTime.now());
        save(event);
    }
}
