package com.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum UserRoleEnum {
    Student("admin"),
    Teacher("teacher"),
    Admin("admin");


    //@EnumValue为mybatisplus的注解，
    // 作用为将枚举值（role本身）保存到数据库中，如UserRoleEnum.Admin为admin。同时在从数据库拿时，admin对应了该类的Admin
    @EnumValue
    private final String role;

    UserRoleEnum(String role) {
        this.role = role;
    }

    /**
     * description
     * 重写toString方法，System.out.println(UserRoleEnum.Admin)可以直接获得 admin
     * author zzq
     * date 2025/12/16 22:37
     * param
     * return
     */
    public String toString() {
        return role;
    }

}
