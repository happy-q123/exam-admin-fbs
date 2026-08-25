package com.domain.entity.relation;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户角色关联表
 *
 * @author zzq
 * @date 2026-06-11
 */
@Data
@TableName("sys_user_role")
public class UserRoleRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private Long roleId;
}
