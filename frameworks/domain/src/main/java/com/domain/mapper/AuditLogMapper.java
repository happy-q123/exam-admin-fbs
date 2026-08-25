package com.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.domain.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志 Mapper 接口
 *
 * @author zzq
 * @date 2026-06-11
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
