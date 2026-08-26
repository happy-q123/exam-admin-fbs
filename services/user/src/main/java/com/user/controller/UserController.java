package com.user.controller;

import com.domain.annotation.Audit;
import com.domain.dto.UserDto;
import com.domain.entity.User;
import com.domain.restful.RestResponse;
import com.user.service.UserOptionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/** 管理端用户查询和状态/角色维护接口，不返回密码字段。 */
@RestController
public class UserController {

    private final UserOptionService userOptionService;

    public UserController(UserOptionService userOptionService) {
        this.userOptionService = userOptionService;
    }

    @GetMapping("/list")
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public List<UserDto> listUsers() {
        return userOptionService.list().stream().map(this::toSafeDto).toList();
    }

    @Audit("修改系统用户")
    @PutMapping("/update")
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public RestResponse<Boolean> updateUser(@RequestBody UserDto dto) {
        if (dto == null || dto.getId() == null) {
            return RestResponse.fail("用户ID不能为空");
        }
        User user = userOptionService.getById(dto.getId());
        if (user == null) {
            return RestResponse.fail("用户不存在");
        }
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }
        if (dto.getNickName() != null) {
            user.setNickName(dto.getNickName());
        }
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
        return RestResponse.success(userOptionService.updateById(user));
    }

    private UserDto toSafeDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickName(user.getNickName())
                .role(user.getRole())
                .status(user.isStatus())
                .build();
    }
}
