package com.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum QuestionTypeEnum {
    //单选题
    SingleOption(0),
    //多选题
    MultiOption(1),
    //判断题
    Judge(2),
    //简答题
    BriefResponse(3);


    //@EnumValue为mybatisplus的注解，
    // 作用为将枚举值（role本身）保存到数据库中，如UserRoleEnum.Admin为admin。同时在从数据库拿时，admin对应了该类的Admin
    @EnumValue
    private final Integer value;

    QuestionTypeEnum(Integer value) {
        this.value = value;
    }

    /**
     * description
     * 重写toString方法，System.out.println()可以直接获得 value
     * author zzq
     * date 2025/12/16 22:37
     * param
     * return
     */
    public String toString() {
        return String.valueOf(value);
    }
}
