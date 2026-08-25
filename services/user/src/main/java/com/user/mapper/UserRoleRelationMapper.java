package com.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.domain.entity.relation.UserRoleRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关联 Mapper 接口
 *
 * @author zzq
 * @date 2026-06-11
 */
@Mapper
public interface UserRoleRelationMapper extends BaseMapper<UserRoleRelation> {
}
