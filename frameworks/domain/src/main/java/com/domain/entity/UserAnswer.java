package com.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户答题明细表
 *
 * @author zzq
 * @date 2026-06-11
 */
@Data
@TableName("user_answer")
public class UserAnswer implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "answer_id", type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long answerId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long recordId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;

    private String userAnswer;

    private Boolean isCorrect;

    private BigDecimal score;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
