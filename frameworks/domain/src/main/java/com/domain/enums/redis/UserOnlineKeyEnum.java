package com.domain.enums.redis;

import com.domain.enums.redis.base.RedisDataTypeEnum;
import com.domain.enums.redis.base.RedisKeyModeEnum;
import lombok.Getter;

import java.util.Arrays;
import java.util.stream.Collectors;
/**
 * description Redis的key类，存储用户在线信息
 * author zzq
 * date 2026/1/7 16:02
 */
@Getter
public enum UserOnlineKeyEnum {

    // 所有在线用户（全局 Set）
    ONLINE_USERS("online-userIds", RedisDataTypeEnum.SET, RedisKeyModeEnum.GLOBAL);

    //key前缀
    private final String prefix;
    //key在redis对应的数据类型
    private final RedisDataTypeEnum dataType;
    //key模式，全局共用还是每个用户独享。
    private final RedisKeyModeEnum keyMode;

    UserOnlineKeyEnum(String prefix,
                      RedisDataTypeEnum dataType,
                      RedisKeyModeEnum keyMode) {
        this.prefix = prefix;
        this.dataType = dataType;
        this.keyMode = keyMode;
    }

    public String buildKey(String... parts) {
        if (keyMode == RedisKeyModeEnum.GLOBAL && parts.length > 0) {
            throw new IllegalStateException("Global key should not have params");
        }
        if (keyMode == RedisKeyModeEnum.BY_USER && parts.length != 1) {
            throw new IllegalStateException("User key requires userId");
        }

        if (parts.length == 0) {
            return prefix;
        }

        return prefix + ":" +
                Arrays.stream(parts)
                        .map(String::valueOf)
                        .collect(Collectors.joining(":"));
    }

}



