package com.domain.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.domain.base.BasePojo;
import com.domain.entity.attribute.ExamSecuritySetting;
import com.domain.entity.attribute.QuestionBody;
import com.domain.entity.relation.ExamQuestionRelation;
import com.domain.enums.QuestionDifficultyEnum;
import com.domain.enums.QuestionTypeEnum;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * description 考试-问题 关系表的dto
 * author zzq
 * date 2025/12/20 16:25
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)//默认不让父类字段参与equals/hashCode，这里显式加上，防止ide提示
public class ExamQuestionRelationDto extends BasePojo {

    /**
     * id
     */
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
     * 问题的分数
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

    public static ExamQuestionRelationDto toDto(ExamQuestionRelation entity) {
        if (entity == null)
            throw new NullPointerException("参数为空");
        return ExamQuestionRelationDto.builder()
                .id(entity.getId())
                .examId(entity.getExamId())
                .questionId(entity.getQuestionId())
                .score(entity.getScore())
                .seq(entity.getSeq())
                .overrideProps(entity.getOverrideProps())
                .build();
    }

    public static List<ExamQuestionRelationDto> toDtoList(List<ExamQuestionRelation> entityList) {
        if (entityList == null)
            throw new NullPointerException("参数为空");
        return entityList.stream().map(ExamQuestionRelationDto::toDto).toList();
    }


}
