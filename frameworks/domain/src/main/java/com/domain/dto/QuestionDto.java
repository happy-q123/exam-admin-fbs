package com.domain.dto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.domain.base.BasePojo;
import com.domain.entity.Question;
import com.domain.entity.attribute.QuestionBody;
import com.domain.enums.QuestionDifficultyEnum;
import com.domain.enums.QuestionTypeEnum;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * description 问题DTO
 * author zzq
 * date 2025/12/20 12:19
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)//默认不让父类字段参与equals/hashCode，这里显式加上，防止ide提示
public class QuestionDto extends BasePojo {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 类型。“单选题”、“多选题”、“简答题”
     */
    private QuestionTypeEnum type;

    /**
     * 难度：0-简单 1-中等 2-困难
     */
    private QuestionDifficultyEnum difficulty;

    /**
     * 创建人id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long creatorId;

    /**
     是否启用
     */
    private Boolean status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 题干、选项、媒体资源等 JSON 内容
     */
    private QuestionBody body;

    /**
     * 标签（对应 PG 的 varchar[]）
     * 注意：对于 PG 数组，通常需要自定义 TypeHandler 或使用 MyBatis-Plus 提供的转换器
     */
    private List<String> tags;

    //最后更新时间，可选择使用mybatisplus的自动填充
    private LocalDateTime latestUpdateTime;

    //最后更新者的id
    @JsonSerialize(using = ToStringSerializer.class)
    private Long latestUpdateId;

    public Question buildForInsert() {
        Assert.notNull(type, "问题类型不能为空");
        Assert.notNull(difficulty, "问题难度不能为空");
        Assert.notNull(creatorId, "问题创建者不能为空");
        Assert.notNull(status, "问题状态不能为空");
        Assert.notNull(body, "问题的体不能为空");
        Assert.notNull(body.getStem(), "问题题干不能为空");
        Assert.notNull(body.getCorrect(), "问题正确答案不能为空");

        //如果不是简单题目，则选项不能为空
        if (!Objects.equals(type.getValue(), QuestionTypeEnum.BriefResponse.getValue()))
            Assert.notNull(body.getOptions(), "问题选项不能为空");

        //如果没有指定最后更新者，则默认为创建者
        if (latestUpdateId== null)
            latestUpdateId = creatorId;

        if (createTime==null)
            createTime=LocalDateTime.now();

        if (latestUpdateTime == null)
            latestUpdateTime =LocalDateTime.now();

        return Question.builder()
                .type(type)
                .difficulty(difficulty)
                .creatorId(creatorId)
                .status(status)
                .createTime(createTime)
                .body(body)
                .tags(tags)
                .latestUpdateTime(latestUpdateTime)
                .latestUpdateId(latestUpdateId)
                .build();
    }


    public static QuestionDto toDto(Question question) {
        return QuestionDto.builder()
                .id(question.getId())
                .type(question.getType())
                .difficulty(question.getDifficulty())
                .creatorId(question.getCreatorId())
                .status(question.isStatus())
                .createTime(question.getCreateTime())
                .body(question.getBody())
                .tags(question.getTags())
                .latestUpdateTime(question.getLatestUpdateTime())
                .latestUpdateId(question.getLatestUpdateId())
                .build();
    }

    public static List<QuestionDto> toDtoList(List<Question> questions) {
        return questions.stream().map(QuestionDto::toDto).toList();
    }
}
