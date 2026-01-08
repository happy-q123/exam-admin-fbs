package com.domain.enums.redis;

import com.domain.enums.redis.base.RedisDataTypeEnum;
import com.domain.enums.redis.base.RedisKeyModeEnum;
import lombok.Getter;

import java.util.Arrays;
import java.util.stream.Collectors;
@Getter
public enum OnlineExamEnum {
    // 用户参加的考试是否正在进行中。key为is:examing:{userId}, value为考试Id
    Is_Examing("is-examing", RedisDataTypeEnum.STRING, RedisKeyModeEnum.PRIVATE),
    // 考试时长。key为exam-expire-time:{examId}, value为考试时长（分钟数）
    Exam_Expire_Time("exam-expire-time",RedisDataTypeEnum.STRING, RedisKeyModeEnum.PRIVATE);

    //key前缀
    private final String prefix;
    //key在redis对应的数据类型
    private final RedisDataTypeEnum dataType;
    //key模式，全局共用还是每个用户独享。
    private final RedisKeyModeEnum keyMode;

    OnlineExamEnum(String prefix,
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
        if (keyMode == RedisKeyModeEnum.PRIVATE && parts.length != 1) {
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
