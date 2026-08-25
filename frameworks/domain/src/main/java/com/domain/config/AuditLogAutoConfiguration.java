package com.domain.config;

import com.domain.aspect.AuditLogAspect;
import com.domain.security.RoleGuard;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.ComponentScan;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * 让引入 domain 模块的业务服务自动启用审计日志切面和 Mapper。
 */
@AutoConfiguration
@ConditionalOnBean(SqlSessionFactory.class)
@MapperScan("com.domain.mapper")
@ComponentScan(basePackageClasses = AuditLogAspect.class)
public class AuditLogAutoConfiguration {

    @org.springframework.context.annotation.Bean("roleGuard")
    public RoleGuard roleGuard() {
        return new RoleGuard();
    }
}
