package com.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "proctor_event", autoResultMap = true)
public class ProctorEvent {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long examId;
    private Long studentId;
    private String eventType;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> payload;
    private LocalDateTime createdTime;
}
