package com.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 错题本表
 *
 * @author zzq
 * @date 2026-06-11
 */
@Data
@TableName("error_book")
public class ErrorBook implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "error_id", type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long errorId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long examId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
