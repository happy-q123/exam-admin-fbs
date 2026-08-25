package com.user.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.dto.UserDto;
import com.domain.entity.User;
import com.domain.enums.UserRoleEnum;
import com.user.mapper.UserMapper;
import com.user.service.UserOptionService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.UUID;

@Service
public class UserOptionServiceImpl extends ServiceImpl<UserMapper, User> implements UserOptionService {
    private final PasswordEncoder passwordEncoder;

    public UserOptionServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean registerUser(UserDto userDto) {

        Assert.hasText(userDto.getUsername(), "用户名不能为空");
        Assert.hasText(userDto.getPassword(), "密码不能为空");
        //生成一个随机匿名
        String randomNickName = UUID.randomUUID()
                + UUID.randomUUID().toString().substring(0, 8);

        if (StringUtils.isBlank(userDto.getNickName())) {
            userDto.setNickName("匿名用户"+randomNickName);
        }
        userDto.setRole(UserRoleEnum.Student);
        userDto.setStatus(true);
        boolean result;
        try {
            User user = userDto.toUser();
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            result=save(user);
        } catch (DuplicateKeyException e) {
            throw new RuntimeException("用户已存在，不可重复注册");
        }
        return result;
    }

    @Override
    public UserDto getUserForLogin(String username) {
        if (StringUtils.isBlank(username))
            throw new RuntimeException("用户名不能为空");

        //根据用户名查询用户表的部分字段
        User user= lambdaQuery().eq(User::getUsername, username)
                .select(User::getId,User::getUsername, User::getPassword, User::getRole, User::isStatus)
                .one();;
        UserDto userDtoForLogin = UserDto.buildForLogin(user);
        return userDtoForLogin;
    }
}
