package com.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
/**
 * description 用于jwk密钥持久化的表
 * author zzq
 * date 2025/12/20 15:03
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_oauth2_jwk")
public class SysOauth2Jwk {
    private String id;
    
    // 对应数据库的 TEXT 字段
    private String jwkJson;
    
    private LocalDateTime createTime;

}