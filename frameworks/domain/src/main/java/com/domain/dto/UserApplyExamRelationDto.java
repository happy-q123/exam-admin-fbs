package com.domain.dto;

import com.domain.base.BasePojo;
import com.domain.entity.relation.UserApplyExamRelation;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;

import java.time.LocalDateTime;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)//默认不让父类字段参与equals/hashCode，这里显式加上，防止ide提示
public class UserApplyExamRelationDto extends BasePojo {
    //id
    // JS的精度 53 位，Long 最大值为 2^63-1，超过这个数字 JS 会丢失精度，因此给前端时，转为字符串
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    //考试id
    // JS的精度 53 位，Long 最大值为 2^63-1，超过这个数字 JS 会丢失精度，因此给前端时，转为字符串
    @JsonSerialize(using = ToStringSerializer.class)
    private Long examId;

    //用户id
    // JS的精度 53 位，Long 最大值为 2^63-1，超过这个数字 JS 会丢失精度，因此给前端时，转为字符串
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    //报名时间
    private LocalDateTime applyTime;

    public UserApplyExamRelation build() {
        return UserApplyExamRelation.builder()
                .id(id)
                .examId(examId)
                .userId(userId)
                .applyTime(applyTime)
                .build();
    }
}
