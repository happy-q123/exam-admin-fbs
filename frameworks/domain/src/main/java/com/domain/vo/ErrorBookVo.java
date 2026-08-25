package com.domain.vo;

import com.domain.entity.attribute.QuestionBody;
import com.domain.enums.QuestionDifficultyEnum;
import com.domain.enums.QuestionTypeEnum;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 错题本展示对象，补充题目内容但不暴露数据库实体字段。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorBookVo {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long errorId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long examId;

    private QuestionTypeEnum type;
    private QuestionDifficultyEnum difficulty;
    private QuestionBody body;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
