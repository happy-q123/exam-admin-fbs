package com.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.domain.entity.relation.RolePermissionRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色权限关联 Mapper 接口
 *
 * @author zzq
 * @date 2026-06-11
 */
@Mapper
public interface RolePermissionRelationMapper extends BaseMapper<RolePermissionRelation> {
}
