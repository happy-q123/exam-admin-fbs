package com.domain.entity.relation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * description 考试-用户 关联表，记录谁报名了考试。
 * author zzq
 * date 2025/12/18 15:37
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value ="user_apply_exam_relation")
public class UserApplyExamRelation {

    //ASSIGN_ID 会自动触发自定义的 CustomIdGenerator
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    //考试id
    private Long examId;

    //用户id
    private Long userId;

    //报名时间
    private LocalDateTime applyTime;
}
