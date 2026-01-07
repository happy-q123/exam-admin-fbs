package com.domain.dto;

import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.domain.base.BasePojo;
import com.domain.entity.relation.UserOnlineExamOptions;
import com.domain.enums.UserOnlineExamOptionTypeEnum;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)//父类字段不参与equals/hashCode
public class UserOnlineExamOptionsDto extends BasePojo {
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
     * 选项类型
     */
    private UserOnlineExamOptionTypeEnum optionType;

    /**
     * 选项时间
     */
    private LocalDateTime optionTime;

    public UserOnlineExamOptions toEntityForSave() {
        Assert.notNull(userId, "用户id不能为空");
        Assert.notNull(examId, "考试id不能为空");
        Assert.notNull(optionType, "用户的操作类型不能为空");
        Assert.notNull(optionTime, "操作的时间不能为空");


        return UserOnlineExamOptions.builder()
                .id(id)
                .userId(userId)
                .examId(examId)
                .optionType(optionType)
                .optionTime(optionTime)
                .build();
    }
}
