package com.user.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.dto.UserDto;
import com.domain.entity.User;
import com.domain.enums.UserRoleEnum;
import com.domain.entity.Permission;
import com.domain.entity.Role;
import com.domain.entity.relation.RolePermissionRelation;
import com.user.service.PermissionService;
import com.user.service.RolePermissionRelationService;
import com.user.service.RoleService;
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
    private final RoleService roleService;
    private final RolePermissionRelationService rolePermissionRelationService;
    private final PermissionService permissionService;

    public UserOptionServiceImpl(PasswordEncoder passwordEncoder,
                                 RoleService roleService,
                                 RolePermissionRelationService rolePermissionRelationService,
                                 PermissionService permissionService) {
        this.passwordEncoder = passwordEncoder;
        this.roleService = roleService;
        this.rolePermissionRelationService = rolePermissionRelationService;
        this.permissionService = permissionService;
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
        if (userDtoForLogin != null && user.getRole() != null) {
            userDtoForLogin.setPermissions(loadPermissionCodes(user.getRole().getRole()));
        }
        return userDtoForLogin;
    }

    private java.util.List<String> loadPermissionCodes(String roleCode) {
        try {
            Role role = roleService.lambdaQuery().eq(Role::getRoleCode, roleCode).one();
            if (role == null) return java.util.List.of();
            java.util.List<Long> permissionIds = rolePermissionRelationService.lambdaQuery()
                    .eq(RolePermissionRelation::getRoleId, role.getRoleId())
                    .list().stream().map(RolePermissionRelation::getPermissionId).toList();
            if (permissionIds.isEmpty()) return java.util.List.of();
            return permissionService.listByIds(permissionIds).stream()
                    .map(Permission::getPermissionCode)
                    .filter(code -> code != null && !code.isBlank())
                    .distinct().toList();
        } catch (RuntimeException exception) {
            // 权限扩展表尚未迁移时不阻断基础登录；角色权限仍由 token 中的业务角色兜底。
            return java.util.List.of();
        }
    }
}
