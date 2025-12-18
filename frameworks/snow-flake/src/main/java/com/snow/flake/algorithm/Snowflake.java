package com.snow.flake.algorithm;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.snow.flake.SnowflakeIdInfo;

import java.io.Serializable;
import java.util.Date;

/**
 * Snowflake 算法分布式唯一 ID 生成器
 * <p>
 * 结构：1bit(符号) + 41bit(时间戳) + 5bit(数据中心) + 5bit(机器ID) + 12bit(序列号)
 * 优势：趋势递增、高性能、不依赖第三方库（除了初始化机器码时）
 *
 * @author Looly (Hutool)
 * @author 12306 Project Team
 * @author ZZQ (注释强化版)
 */
public class Snowflake implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 算法起始时间点：2025-12-18 21:28:00 (一旦确定不可修改，否则旧ID与新ID会重复)
     */
    private static long DEFAULT_TWEPOCH = 1766064480483L;

    /**
     * 默认允许的时间回拨阈值（2000毫秒）。
     * 解决分布式环境下 NTP 同步导致服务器时间微小回跳的问题。
     */
    private static long DEFAULT_TIME_OFFSET = 2000L;

    // 各部分占用的位数
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATA_CENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    // 各部分最大值计算：通过位移运算得到全1的二进制数
    // 例如：-1L ^ (-1L << 5) = 31
    private static final long MAX_WORKER_ID = -1L ^ (-1L << WORKER_ID_BITS);
    private static final long MAX_DATA_CENTER_ID = -1L ^ (-1L << DATA_CENTER_ID_BITS);
    // 序列号掩码：用于保证序列号在 0-4095 之间循环
    private static final long SEQUENCE_MASK = -1L ^ (-1L << SEQUENCE_BITS);

    // 各部分位移偏移量
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;

    // --- Final 变量：必须在构造函数中全部完成初始化 ---
    private final long twepoch;            // 起始时间戳
    private final long workerId;          // 机器标识ID
    private final long dataCenterId;      // 数据中心ID
    private final boolean useSystemClock; // 是否使用优化版系统时钟
    private final long timeOffset;        // 容忍的回拨毫秒数
    private final long randomSequenceLimit; // 随机序号上限（优化低频并发下的数据分布）

    private long sequence = 0L;           // 毫秒内自增序列
    private long lastTimestamp = -1L;     // 上次生成ID的时间戳

    // ---------------------------------------------------------------- 构造函数

    /**
     * 默认构造：自动探测并获取本机可用的机器ID和数据中心ID
     */
    public Snowflake() {
        this(IdUtil.getWorkerId(IdUtil.getDataCenterId(MAX_DATA_CENTER_ID), MAX_WORKER_ID));
    }

    /**
     * @param workerId 工作机器ID (0~31)
     */
    public Snowflake(long workerId) {
        this(workerId, IdUtil.getDataCenterId(MAX_DATA_CENTER_ID));
    }

    /**
     * @param workerId     工作机器ID (0~31)
     * @param dataCenterId 数据中心ID (0~31)
     */
    public Snowflake(long workerId, long dataCenterId) {
        this(workerId, dataCenterId, false);
    }

    /**
     * @param workerId         工作机器ID (0~31)
     * @param dataCenterId     数据中心ID (0~31)
     * @param isUseSystemClock 是否使用 SystemClock.now() 提高获取时间戳的性能
     */
    public Snowflake(long workerId, long dataCenterId, boolean isUseSystemClock) {
        // 调用核心构造函数，使用默认的起始时间和回拨阈值
        this(null, workerId, dataCenterId, isUseSystemClock, DEFAULT_TIME_OFFSET, 0);
    }

    /**
     * 核心全参数构造函数
     *
     * @param epochDate           起始时间（null则使用默认值）
     * @param workerId            工作机器ID
     * @param dataCenterId        数据中心ID
     * @param isUseSystemClock    是否使用系统时钟优化
     * @param timeOffset          允许回拨的毫秒数
     * @param randomSequenceLimit 随机序号上限（0表示不开启）
     */
    public Snowflake(Date epochDate, long workerId, long dataCenterId, boolean isUseSystemClock, long timeOffset, long randomSequenceLimit) {
        this.twepoch = (null != epochDate) ? epochDate.getTime() : DEFAULT_TWEPOCH;
        this.workerId = Assert.checkBetween(workerId, 0, MAX_WORKER_ID);
        this.dataCenterId = Assert.checkBetween(dataCenterId, 0, MAX_DATA_CENTER_ID);
        this.useSystemClock = isUseSystemClock;
        this.timeOffset = timeOffset;
        this.randomSequenceLimit = Assert.checkBetween(randomSequenceLimit, 0, SEQUENCE_MASK);
    }

    // ---------------------------------------------------------------- 核心业务方法

    /**
     * 核心方法：获取下一个唯一ID
     * 使用 synchronized 保证多线程环境下的原子性
     *
     * @return 64位分布式唯一ID
     */
    public synchronized long nextId() {
        long timestamp = genTime();

        // 1. 检查时钟回拨
        if (timestamp < this.lastTimestamp) {
            long offset = this.lastTimestamp - timestamp;
            if (offset < timeOffset) {
                // 如果回拨在容忍范围内，强制停留在上次的时间戳生成，避免报错
                timestamp = lastTimestamp;
            } else {
                // 回拨跨度太大，抛出异常防止ID冲突
                throw new IllegalStateException(StrUtil.format("系统时钟回拨，拒绝在 {}ms 内生成ID", offset));
            }
        }

        // 2. 处理相同毫秒内的并发请求
        if (timestamp == this.lastTimestamp) {
            // 在当前毫秒内自增序列，并通过掩码防止超过 4095
            final long nextSequence = (this.sequence + 1) & SEQUENCE_MASK;
            if (nextSequence == 0) {
                // 如果当前毫秒序列已满，循环等待直到下一毫秒
                timestamp = tilNextMillis(lastTimestamp);
            }
            this.sequence = nextSequence;
        } else {
            // 3. 进入了新的毫秒
            if (randomSequenceLimit > 1) {
                // 优化：低并发下起始序号随机，防止ID总是偶数导致数据库分表不均
                sequence = RandomUtil.randomLong(randomSequenceLimit);
            } else {
                sequence = 0L;
            }
        }

        lastTimestamp = timestamp;

        // 4. 位运算组装：将时间戳、机房ID、机器ID、序列号拼装成64位Long
        return ((timestamp - twepoch) << TIMESTAMP_LEFT_SHIFT) // 时间戳向左移动22位
                | (dataCenterId << DATA_CENTER_ID_SHIFT)       // 数据中心移动17位
                | (workerId << WORKER_ID_SHIFT)               // 机器标识移动12位
                | sequence;                                    // 序列号占据最后12位
    }

    /**
     * 自旋等待：强制等待直到系统时间进入下一毫秒
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = genTime();
        while (timestamp <= lastTimestamp) {
            timestamp = genTime();
        }
        return timestamp;
    }

    /**
     * 获取当前系统时间戳
     */
    private long genTime() {
        return this.useSystemClock ? SystemClock.now() : System.currentTimeMillis();
    }

    /**
     * 解析雪花ID：从一个已生成的数字ID中反向提取出它的组成信息
     *
     * @param snowflakeId 生成的雪花ID
     * @return 包含时间戳、机器ID等信息的对象
     */
    public SnowflakeIdInfo parseSnowflakeId(long snowflakeId) {
        return SnowflakeIdInfo.builder()
                .sequence((int) (snowflakeId & SEQUENCE_MASK)) // 与掩码进行“位与”，提取末尾12位
                .workerId((int) ((snowflakeId >> WORKER_ID_SHIFT) & MAX_WORKER_ID)) // 右移12位后提取机器ID
                .dataCenterId((int) ((snowflakeId >> DATA_CENTER_ID_SHIFT) & MAX_DATA_CENTER_ID)) // 右移17位后提取机房ID
                .timestamp((snowflakeId >> TIMESTAMP_LEFT_SHIFT) + twepoch) // 右移22位提取时间戳并还原
                .build();
    }
}