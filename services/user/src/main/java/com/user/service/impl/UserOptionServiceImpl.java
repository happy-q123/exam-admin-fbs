package com.user.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.dto.UserDto;
import com.domain.entity.User;
import com.domain.enums.UserRoleEnum;
import com.user.mapper.UserMapper;
import com.user.service.UserOptionService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Random;
import java.util.UUID;

@Service
public class UserOptionServiceImpl extends ServiceImpl<UserMapper, User> implements UserOptionService {
    @Override
    public boolean registerUser(UserDto userDto) {

        Assert.hasText(userDto.getUsername(), "用户名不能为空");
        Assert.hasText(userDto.getPassword(), "密码不能为空");
        //时间戳作为id
        //todo 雪花算法
        userDto.setId(System.currentTimeMillis());

        //生成一个随机匿名
        String randomNickName = new Random().nextLong() + userDto.getId()
                + UUID.randomUUID().toString().substring(0, 8);

        userDto.setNickName("匿名用户"+randomNickName);
        userDto.setRole(UserRoleEnum.Student);
        return save(userDto.toUser());
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
