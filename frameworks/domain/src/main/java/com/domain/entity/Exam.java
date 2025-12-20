package com.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.domain.entity.attribute.ExamSecuritySetting;
import lombok.*;

import java.time.LocalDateTime;
/**
 * description 考试表
 * author zzq
 * date 2025/12/18 15:07
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName(value = "exam", autoResultMap = true) // 必须开启 autoResultMap
public class Exam {
    //ASSIGN_ID 会自动触发自定义的 CustomIdGenerator
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    //考试标题
    private String title;

    //考试介绍
    private String introduce;

    // 开始时间，对应 PG 的 timestamp
    private LocalDateTime beginTime;

    // 持续时间，单位为分钟
    private Integer durationTime;

    // 指定 typeHandler
    @TableField(typeHandler = JacksonTypeHandler.class,value = "security_setting")
    private ExamSecuritySetting securitySetting;

    //考试状态，true 表示启用，false 表示禁用
    private Boolean status;

    //创建时间，对应 PG 的 timestamp
    private LocalDateTime createTime;

    //更新时间，对应 PG 的 timestamp
    private LocalDateTime latestUpdateTime;

    // 最大考试人数
    private Integer maxUserNum;

    // 剩余考试人数
    private Integer restUserNum;

    // 考试的创建者id
    private Long creator;
}