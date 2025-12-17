package com.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domain.dto.UserDto;
import com.domain.entity.User;

public interface UserOptionService extends IService<User> {

    /**
     * description 用户注册
     * author zzq
     * date 2025/12/16 23:10
     * param
     * return
     */
    UserDto registerUser(UserDto userDto);


    /**
     * description 用户登录
     * author zzq
     * date 2025/12/17 14:20
     * param
     * return
     */
    UserDto getUserForLogin(String username);
}
