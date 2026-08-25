package com.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.domain.entity.Role;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色 Mapper 接口
 *
 * @author zzq
 * @date 2026-06-11
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}
