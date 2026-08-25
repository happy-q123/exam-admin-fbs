package com.domain.entity.relation;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色权限关联表
 *
 * @author zzq
 * @date 2026-06-11
 */
@Data
@TableName("sys_role_permission")
public class RolePermissionRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long roleId;

    private Long permissionId;
}
