package com.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.domain.entity.Permission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 权限 Mapper 接口
 *
 * @author zzq
 * @date 2026-06-11
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
}
