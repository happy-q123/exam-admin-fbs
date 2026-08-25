package com.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 权限实体类
 *
 * @author zzq
 * @date 2026-06-11
 */
@Data
@TableName("sys_permission")
public class Permission implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "permission_id", type = IdType.AUTO)
    private Long permissionId;

    private Long parentId;

    private String permissionName;

    private String permissionCode;

    private String path;

    /**
     * 1: 菜单 2: 按钮 3: 接口
     */
    private Integer type;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
