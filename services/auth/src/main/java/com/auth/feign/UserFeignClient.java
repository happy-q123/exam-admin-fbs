package com.auth.feign;

import com.domain.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * description 向user-service服务发起相关请求
 * author zzq
 * date 2025/12/16 21:02
 */
@FeignClient(name = "user-service")
public interface UserFeignClient {

    //调用user-service服务的/user/common/getUserByUsername接口
    @GetMapping("/user/common/getUserByUsername/{username}")
    UserDto loadUserByUsername(@PathVariable("username") String username);

}