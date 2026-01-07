package com.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.domain.base.BasePojo;
import com.domain.entity.attribute.UserOnlineExamQuestionAnswerBody;
import com.domain.entity.relation.UserOnlineExamAnswer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;
import org.springframework.util.Assert;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)//父类字段不参与equals/hashCode
public class UserOnlineExamAnswerDto extends BasePojo {
    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 考试ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long examId;

    /**
     * 问题ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;

    /**
     * 用户答案
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private UserOnlineExamQuestionAnswerBody answer;

    /**
     * 得分
     */
    private Float score;

    /**
     * 是否正确
     */
    private Boolean isCorrect;

    /**
     * 答题时间
     */
    private LocalDateTime optionTime;

    public UserOnlineExamAnswer toEntityForSave() {
        Assert.notNull(userId, "用户id不能为空");
        Assert.notNull(examId, "考试id不能为空");
        Assert.notNull(questionId, "问题id");
        Assert.notNull(answer, "答案不能为空");
        Assert.notNull(optionTime, "答题时间不能为空");


        return UserOnlineExamAnswer.builder()
                .id(id)
                .userId(userId)
                .examId(examId)
                .questionId(questionId)
                .answer(answer)
                .score(null)
                .isCorrect(null)
                .optionTime(optionTime)
                .build();
    }

    //todo 原本想着留着这个判断表id是否为空，但是表中有“联合主键”索引，依据用户、考试、问题id就能判断是否存在。
    public UserOnlineExamAnswer toEntityForUpdate() {
        Assert.notNull(userId, "用户id不能为空");
        Assert.notNull(examId, "考试id不能为空");
        Assert.notNull(questionId, "问题id");
        Assert.notNull(answer, "答案不能为空");
        Assert.notNull(optionTime, "答题时间不能为空");


        return UserOnlineExamAnswer.builder()
                .id(id)
                .userId(userId)
                .examId(examId)
                .questionId(questionId)
                .answer(answer)
                .score(null)
                .isCorrect(null)
                .optionTime(optionTime)
                .build();
    }
}
