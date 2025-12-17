package com.user.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.dto.UserDto;
import com.domain.entity.User;
import com.user.mapper.UserMapper;
import com.user.service.UserOptionService;
import org.springframework.stereotype.Service;

@Service
public class UserOptionServiceImpl extends ServiceImpl<UserMapper, User> implements UserOptionService {
    @Override
    public UserDto registerUser(UserDto userDto) {
        return null;
    }

    @Override
    public UserDto getUserForLogin(String username) {
        if (StringUtils.isBlank(username))
            throw new RuntimeException("用户名不能为空");

        //根据用户名查询用户表的部分字段
        User user= lambdaQuery().eq(User::getUsername, username)
                .select(User::getId,User::getUsername, User::getPassword, User::getRole)
                .one();;
        UserDto userDtoForLogin = UserDto.buildForLogin(user);
        return userDtoForLogin;
    }
}
