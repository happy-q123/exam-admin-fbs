package com.snow.flake;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 雪花算法ID解析信息对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnowflakeIdInfo {
    /**
     * 毫秒级时间戳
     */
    private Long timestamp;

    /**
     * 工作机器ID
     */
    private Integer workerId;

    /**
     * 数据中心ID
     */
    private Integer dataCenterId;

    /**
     * 毫秒内序列号
     */
    private Integer sequence;
}