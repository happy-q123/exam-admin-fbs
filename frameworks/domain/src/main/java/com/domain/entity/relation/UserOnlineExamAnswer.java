package com.domain.entity.relation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.domain.entity.attribute.UserOnlineExamQuestionAnswerBody;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value ="user_online_exam_answer",autoResultMap = true)
public class UserOnlineExamAnswer {
    /**
     * 主键ID
     */
    //ASSIGN_ID 会自动触发自定义的 CustomIdGenerator
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 考试ID
     */
    private Long examId;
    
    /**
     * 问题ID
     */
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
}
