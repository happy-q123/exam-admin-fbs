package com.domain.vo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.domain.dto.ExamQuestionRelationDto;
import com.domain.dto.QuestionDto;
import com.domain.dto.UserDto;
import com.domain.entity.attribute.QuestionBody;
import com.domain.enums.QuestionDifficultyEnum;
import com.domain.enums.QuestionTypeEnum;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * description 将 问题 表和 考试-问题 表的某些字段组合。
 * author zzq
 * date 2025/12/20 16:41
 * param
 * return
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExamQuestionRelationVo {
    /**
     * 考试-问题关系id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long examQuestionRelationId;
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

    /**
     * 类型。“单选题”、“多选题”、“简答题”
     */
    private QuestionTypeEnum type;

    /**
     * 难度：0-简单 1-中等 2-困难
     */
    private QuestionDifficultyEnum difficulty;

    /**
     是否启用
     */
    private Boolean status;

    /**
     * 题干、选项、媒体资源等 JSON 内容
     */
    private QuestionBody body;

    /**
     * 问题标签（对应 PG 的 varchar[]）
     */
    private List<String> tags;

    //最后更新时间，可选择使用mybatisplus的自动填充
    private LocalDateTime latestUpdateTime;

    //最后更新者的id
    @JsonSerialize(using = ToStringSerializer.class)
    private Long latestUpdateId;

    /**
     * 创建人id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long creatorId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public static ExamQuestionRelationVo toVo(ExamQuestionRelationDto relationDto, QuestionDto questionDto){
        if (relationDto == null || questionDto == null)
            throw new RuntimeException("参数为空");

        return ExamQuestionRelationVo.builder()
                .examQuestionRelationId(relationDto.getId())
                .examId(relationDto.getExamId())
                .questionId(questionDto.getId())
                .score(relationDto.getScore())
                .seq(relationDto.getSeq())
                .overrideProps(relationDto.getOverrideProps())
                .type(questionDto.getType())
                .difficulty(questionDto.getDifficulty())
                .status(questionDto.getStatus())
                .body(questionDto.getBody())
                .tags(questionDto.getTags())
                .latestUpdateTime(questionDto.getLatestUpdateTime())
                .latestUpdateId(questionDto.getLatestUpdateId())
                .creatorId(questionDto.getCreatorId())
                .createTime(questionDto.getCreateTime())
                .build();
    }
    public static List<ExamQuestionRelationVo> toVoList(List<ExamQuestionRelationDto> relationDtoList,
                                                        List<QuestionDto> questionDtoList){
        if (relationDtoList == null || questionDtoList == null)
            throw new RuntimeException("参数为空");
        if (relationDtoList.size() != questionDtoList.size())
            throw new RuntimeException("参数长度不一致");

        return relationDtoList.stream()
                .map(relationDto -> toVo(relationDto, questionDtoList.get(relationDtoList.indexOf(relationDto))))
                .toList();

    }
}
