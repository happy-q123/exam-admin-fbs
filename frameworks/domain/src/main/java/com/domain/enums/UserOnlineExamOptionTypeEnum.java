package com.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * description 用户在线考试操作类型枚举
 * author zzq
 * date 2026/1/7 15:33
 */
public enum UserOnlineExamOptionTypeEnum {
    //进入考试
    Enter("enter"),
    //中途退出考试
    Exit("exit"),
    //提交考试
    Commit("commit");

    //@EnumValue为mybatisplus的注解，
    // 作用为将枚举值（role本身）保存到数据库中，如UserRoleEnum.Admin为admin。同时在从数据库拿时，admin对应了该类的Admin
    @EnumValue
    @JsonValue
    private final String optionType;

    UserOnlineExamOptionTypeEnum(String optionType) {
        this.optionType = optionType;
    }

    /**
     * description
     写toString方法，System.out.println()可以直接获得 admin
     * author zzq
     * date 2026/1/7 15:32
     */
    public String toString() {
        return optionType;
    }
}
