package com.user.controller;

import com.domain.entity.Role;
import com.domain.entity.relation.RolePermissionRelation;
import com.domain.annotation.Audit;
import com.user.service.RoleService;
import com.user.service.RolePermissionRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/role")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @Autowired
    private RolePermissionRelationService rolePermissionRelationService;

    @GetMapping("/list")
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public List<Role> listRoles() {
        return roleService.list();
    }

    @Audit("新增系统角色")
    @PostMapping("/add")
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public boolean addRole(@RequestBody Role role) {
        return roleService.save(role);
    }

    @Audit("修改系统角色")
    @PutMapping("/update")
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public boolean updateRole(@RequestBody Role role) {
        return roleService.updateById(role);
    }

    @Audit("删除系统角色")
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public boolean deleteRole(@PathVariable Long id) {
        return roleService.removeById(id);
    }

    @GetMapping("/permissionIds")
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public List<Long> permissionIds(@RequestParam Long roleId) {
        if (roleId == null || roleService.getById(roleId) == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        return rolePermissionRelationService.lambdaQuery()
                .eq(RolePermissionRelation::getRoleId, roleId)
                .list()
                .stream()
                .map(RolePermissionRelation::getPermissionId)
                .toList();
    }

    @Audit("分配角色权限")
    @PostMapping("/assignPermissions")
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public boolean assignPermissions(@RequestParam Long roleId, @RequestBody List<Long> permissionIds) {
        if (roleId == null || roleService.getById(roleId) == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        rolePermissionRelationService.remove(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RolePermissionRelation>()
                        .eq("role_id", roleId));
        if (permissionIds == null || permissionIds.isEmpty()) {
            return true;
        }
        List<RolePermissionRelation> relations = permissionIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .map(permissionId -> {
                    RolePermissionRelation relation = new RolePermissionRelation();
                    relation.setRoleId(roleId);
                    relation.setPermissionId(permissionId);
                    return relation;
                })
                .toList();
        return rolePermissionRelationService.saveBatch(relations);
    }
}
