package com.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.entity.relation.UserRoleRelation;
import com.user.mapper.UserRoleRelationMapper;
import com.user.service.UserRoleRelationService;
import org.springframework.stereotype.Service;

/**
 * 用户角色关联服务实现类
 *
 * @author zzq
 * @date 2026-06-11
 */
@Service
public class UserRoleRelationServiceImpl extends ServiceImpl<UserRoleRelationMapper, UserRoleRelation> implements UserRoleRelationService {
}
