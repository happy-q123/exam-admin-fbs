package com.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计日志表
 *
 * @author zzq
 * @date 2026-06-11
 */
@Data
@TableName("audit_log")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;

    private Long userId;

    private String action;

    private String ipAddress;

    private String requestParams;

    private Long duration;

    private LocalDateTime createTime;
}
