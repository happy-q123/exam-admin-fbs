package com.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.entity.Permission;
import com.user.mapper.PermissionMapper;
import com.user.service.PermissionService;
import org.springframework.stereotype.Service;

/**
 * 权限服务实现类
 *
 * @author zzq
 * @date 2026-06-11
 */
@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {
}
