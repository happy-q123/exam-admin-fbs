package com.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.entity.Role;
import com.user.mapper.RoleMapper;
import com.user.service.RoleService;
import com.domain.entity.relation.RolePermissionRelation;
import com.user.service.RolePermissionRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 角色服务实现类
 *
 * @author zzq
 * @date 2026-06-11
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Autowired
    private RolePermissionRelationService rolePermissionRelationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignPermissions(Long roleId, List<Long> permissionIds) {
        if (roleId == null || getById(roleId) == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        rolePermissionRelationService.remove(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RolePermissionRelation>()
                        .eq("role_id", roleId));
        if (permissionIds == null || permissionIds.isEmpty()) {
            return true;
        }
        List<RolePermissionRelation> relations = permissionIds.stream()
                .filter(Objects::nonNull)
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
