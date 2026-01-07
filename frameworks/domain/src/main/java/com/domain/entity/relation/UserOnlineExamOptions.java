package com.domain.entity.relation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.domain.enums.UserOnlineExamOptionTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value ="user_online_exam_options")
public class UserOnlineExamOptions {
    /**
     * 主键ID，使用 ASSIGN_ID 策略，会自动触发自定义的 CustomIdGenerator
     */
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
     * 选项类型
     */
    private UserOnlineExamOptionTypeEnum optionType;
    
    /**
     * 选项时间
     */
    private LocalDateTime optionTime;
}
