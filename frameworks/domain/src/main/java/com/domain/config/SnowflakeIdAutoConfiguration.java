package com.domain.config;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.snow.flake.algorithm.Snowflake;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * description 雪花算法配置bean
    如果业务服务没有配置，则自动加载这个配置
    业务服务和当前模块报名不完全一致，因此需要在该模块resource/Metadata-INF/spring/目录下，
    建立文件org.springframework.boot.autoconfigure.AutoConfiguration.imports配置。
 * author zzq
 * date 2025/12/18 23:08
 */
@Configuration
@AutoConfigureBefore(name = "com.baomidou.mybatisplus.autoconfigure.IdentifierGeneratorAutoConfiguration")
public class SnowflakeIdAutoConfiguration {

    /**
     * 如果业务模块没有配置 Snowflake Bean，则这里提供默认配置
     */
    @Bean
    @ConditionalOnMissingBean(Snowflake.class)
    public Snowflake snowflake(@Value("${vrs.snowflake.worker-id:0}") long workerId,
                               @Value("${vrs.snowflake.datacenter-id:0}") long datacenterId) {

        // 默认 workerId 和 dataCenterId 设为 0
        return new Snowflake(workerId, datacenterId);
    }

    /**
     * 核心：如果检测到 Spring 容器中还没有 IdentifierGenerator 类型的 Bean，
     * 则配置我们自定义的雪花 ID 生成器。
     */
    @Bean
    @ConditionalOnMissingBean(IdentifierGenerator.class)
    public IdentifierGenerator identifierGenerator(Snowflake snowflake) {
        return new IdentifierGenerator() {
            @Override
            public Long nextId(Object entity) {
                return snowflake.nextId();
            }
        };
    }
}