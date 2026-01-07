package com.domain.enums.redis.base;

public enum RedisKeyModeEnum {
    GLOBAL,     // 全局唯一 key，不需要 id
    BY_USER     // 按 userId 分片
}
