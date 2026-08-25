package com.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.entity.relation.RolePermissionRelation;
import com.user.mapper.RolePermissionRelationMapper;
import com.user.service.RolePermissionRelationService;
import org.springframework.stereotype.Service;

/**
 * 角色权限关联服务实现类
 *
 * @author zzq
 * @date 2026-06-11
 */
@Service
public class RolePermissionRelationServiceImpl extends ServiceImpl<RolePermissionRelationMapper, RolePermissionRelation> implements RolePermissionRelationService {
}
