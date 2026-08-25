package com.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.entity.Role;
import com.user.mapper.RoleMapper;
import com.user.service.RoleService;
import org.springframework.stereotype.Service;

/**
 * 角色服务实现类
 *
 * @author zzq
 * @date 2026-06-11
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {
}
