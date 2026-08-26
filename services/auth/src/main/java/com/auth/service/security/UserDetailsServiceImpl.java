package com.auth.service.security;

import com.auth.dto.CustomSecurityUser;
import com.auth.feign.UserFeignClient;
import com.domain.dto.UserDto;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
/**
 * description
 * SpringSecurity需要实现的UserDetails类，该类的方法允许去数据库查询用户的密码和权限。
 * author zzq
 * date 2025/12/16 21:09
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserFeignClient userFeignClient;

    public UserDetailsServiceImpl(UserFeignClient userFeignClient) {
        // 注入 Feign 客户端
        this.userFeignClient = userFeignClient;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        com.domain.restful.RestResponse<UserDto> response;
        try {
            response = userFeignClient.loadUserByUsername(username);
        } catch (Exception e) {
            throw new UsernameNotFoundException("查询用户信息失败: " + e.getMessage());
        }

        if (response == null || response.getData() == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        UserDto userResult = response.getData();
        if (!Boolean.TRUE.equals(userResult.getStatus())) {
            throw new UsernameNotFoundException("用户已被禁用");
        }
        List<String> authorities = new ArrayList<>();
        if (userResult.getRole() != null) {
            authorities.add(userResult.getRole().toString());
        } else {
            authorities.add("student");
        }
        if (userResult.getPermissions() != null) {
            userResult.getPermissions().stream()
                    .filter(permission -> permission != null && !permission.isBlank())
                    .map(permission -> "PERM_" + permission)
                    .forEach(authorities::add);
        }
        return new CustomSecurityUser(username, userResult.getPassword(),
                userResult.getId(), authorities.toArray(String[]::new));
    }
}
