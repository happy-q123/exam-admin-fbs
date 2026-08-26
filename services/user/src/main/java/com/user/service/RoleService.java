package com.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domain.entity.Role;

/**
 * 角色服务类
 *
 * @author zzq
 * @date 2026-06-11
 */
public interface RoleService extends IService<Role> {
    boolean assignPermissions(Long roleId, java.util.List<Long> permissionIds);
}
