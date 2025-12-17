package com.domain.dto;

import com.domain.entity.User;
import com.domain.enums.UserRoleEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Integer id;
    private String username;
    private String password;
    private String nickName;
    private UserRoleEnum role;

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
}
