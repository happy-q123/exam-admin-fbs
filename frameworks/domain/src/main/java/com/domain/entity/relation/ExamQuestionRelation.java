package com.domain.entity.relation;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
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
    private Long id;

    /**
     * 考试id
     */
    private Long examId;

    /**
     * 问题id
     */
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
     * 题的分组。
     * 注意：group 是 SQL 保留字，必须在 value 中指明字段名
     * todo 枚举
     */
    private String questionGroup;

    /**
     * 覆盖配置。用于存储“只针对本次考试生效”的属性
     * 映射为 Map 比较灵活，也可以定义专门的 DTO
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> overrideProps;
}