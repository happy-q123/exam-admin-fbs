package com.user.controller;

import com.domain.entity.Permission;
import com.domain.annotation.Audit;
import com.user.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/permission")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @GetMapping("/list")
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public List<Permission> listPermissions() {
        return permissionService.list();
    }

    @Audit("新增菜单或权限")
    @PostMapping("/add")
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public boolean addPermission(@RequestBody Permission permission) {
        return permissionService.save(permission);
    }

    @Audit("修改菜单或权限")
    @PutMapping("/update")
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public boolean updatePermission(@RequestBody Permission permission) {
        return permissionService.updateById(permission);
    }

    @Audit("删除菜单或权限")
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public boolean deletePermission(@PathVariable("id") Long id) {
        return permissionService.removeById(id);
    }
}
