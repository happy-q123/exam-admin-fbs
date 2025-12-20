package com.domain.dto;

import com.domain.base.BasePojo;
import com.domain.entity.User;
import com.domain.enums.UserRoleEnum;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;
import org.springframework.util.Assert;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)//父类字段不参与equals/hashCode
public class UserDto extends BasePojo {
    // JS的精度 53 位，Long 最大值为 2^63-1，超过这个数字 JS 会丢失精度，因此给前端时，转为字符串
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String username;
    private String password;
    private String nickName;
    private UserRoleEnum role;
    private boolean status;

    public static UserDto buildForLogin(User user){
        if(user == null){
            return null;
        }
        String username = user.getUsername();
        String password = user.getPassword();
        UserRoleEnum role = user.getRole();

        Assert.hasText(user.getUsername(), "用户名不能为空");
        Assert.hasText(user.getPassword(), "密码不能为空");
        Assert.notNull(user.getId(), "id不能为空");
        Assert.notNull(user.getRole(), "权限不能为空");

        UserDto userDtoForLogin = UserDto.builder()
                .username(username)
                .password(password)
                .role(role)
                .id(user.getId())
                .build();
        return userDtoForLogin;
    }

    public User toUser(){
        return User.builder()
                .id(id)
                .username(username)
                .password(password)
                .nickName(nickName)
                .role(role)
                .build();
    }
}
