package com.domain.enums.redis.base;

public enum RedisKeyModeEnum {
    GLOBAL,     // 无序拼接直接使用
    PRIVATE     // 需要特殊拼接，如key的前缀拼接用户ID
}
