package com.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.domain.enums.UserRoleEnum;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@TableName("sys_user")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    //ASSIGN_ID 会自动触发自定义的 CustomIdGenerator
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    //用户账号
    private String username;
    private String password;

    //用户昵称
    private String nickName;
    //用户角色（权限）
    private UserRoleEnum role;

    //用户状态，true 启用，false 禁用
    private boolean status;
}
