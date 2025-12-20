package com.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.domain.dto.QuestionDto;
import com.domain.entity.attribute.QuestionBody;
import com.domain.enums.QuestionDifficultyEnum;
import com.domain.enums.QuestionTypeEnum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * description 题目实体
 * author zzq
 * date 2025/12/19 16:52
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName(value = "question", autoResultMap = true)
public class Question {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 类型。“单选题”、“多选题”、“简答题”
     */
    private QuestionTypeEnum type;

    /**
     * 难度：0-简单 1-一般 2-困难
     */
    private QuestionDifficultyEnum difficulty;

    /**
     * 创建人id
     */
    private Long creatorId;

    /**
        是否启用
     */
    private boolean status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 题干、选项、媒体资源等 JSON 内容
     * 必须指定 typeHandler 才能实现 对象 <-> JSONB 的互转
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private QuestionBody body;

    /**
     * 标签
     * 数据库存为 JSON 格式：["标签A", "标签B"]
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    //最后更新时间，可选择使用mybatisplus的自动填充
    private LocalDateTime latestUpdateTime;

    //最后更新者的id
    private Long latestUpdateId;

}
