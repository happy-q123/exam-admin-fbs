package com.domain.entity.relation;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * description 考试题目关联表
 * author zzq
 * date 2025/12/19 17:15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "exam_question_relation", autoResultMap = true)
public class ExamQuestionRelation {

    /**
     * id
    */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 考试id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long examId;

    /**
     * 问题id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;

    /**
     * 问题的分数 (numeric(5,1) 对应 BigDecimal)
     */
    private BigDecimal score;

    /**
     * 问题的序号
     */
    private Integer seq;

    /**
     * 覆盖配置。用于存储“只针对本次考试生效”的属性
     * 映射为 Map 比较灵活，也可以定义专门的 DTO
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> overrideProps;
}
