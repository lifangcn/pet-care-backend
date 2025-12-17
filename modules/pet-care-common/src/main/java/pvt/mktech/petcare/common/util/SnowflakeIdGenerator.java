package pvt.mktech.petcare.common.util;

/**
 * Twitter的Snowflake算法实现，用于生成全局唯一ID。
 * 简化代码，自定义实现 详见
 * @link DistributedIdGenerator#generateId()
 */
@Deprecated
//@Component
public class SnowflakeIdGenerator {
    // 开始时间戳2025-01-01 00:00:00（可以调整为项目开始时间）
    private static final long BEGIN_TIMESTAMP = 1735689600L;
    // 每一部分占用的位数
    private final static long SEQUENCE_BIT = 12; // 序列号占用的位数
    private final static long MACHINE_BIT = 5;   // 机器标识占用的位数
    private final static long DATA_CENTER_BIT = 5; // 数据中心占用的位数

    // 每一部分的最大值
    private final static long MAX_DATA_CENTER_NUM = ~(-1L << DATA_CENTER_BIT);
    private final static long MAX_MACHINE_NUM = ~(-1L << MACHINE_BIT);
    private final static long MAX_SEQUENCE = ~(-1L << SEQUENCE_BIT);

    // 每一部分向左的位移
    private final static long MACHINE_LEFT = SEQUENCE_BIT;
    private final static long DATA_CENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;
    private final static long TIMESTAMP_LEFT = DATA_CENTER_LEFT + DATA_CENTER_BIT;

    private final long dataCenterId;  // 数据中心
    private final long machineId;     // 机器标识
    private long sequence = 0L; // 序列号
    private long lastStamp = -1L; // 上一次时间戳

    public SnowflakeIdGenerator() {
        // 这里可以从配置文件中读取，暂时写死
        this.dataCenterId = 1L;
        this.machineId = 1L;
    }

    // 产生下一个ID
    public synchronized long nextId() {
        long currStamp = getNewStamp();
        if (currStamp < lastStamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id");
        }

        if (currStamp == lastStamp) {
            // 相同毫秒内，序列号自增
            sequence = (sequence + 1) & MAX_SEQUENCE;
            // 同一毫秒的序列数已经达到最大
            if (sequence == 0L) {
                currStamp = getNextMill();
            }
        } else {
            // 不同毫秒内，序列号置为0
            sequence = 0L;
        }

        lastStamp = currStamp;

        return (currStamp - BEGIN_TIMESTAMP) << TIMESTAMP_LEFT // 时间戳部分
                | dataCenterId << DATA_CENTER_LEFT       // 数据中心部分
                | machineId << MACHINE_LEFT             // 机器标识部分
                | sequence;                             // 序列号部分
    }

    private long getNextMill() {
        long mill = getNewStamp();
        while (mill <= lastStamp) {
            mill = getNewStamp();
        }
        return mill;
    }

    private long getNewStamp() {
        return System.currentTimeMillis();
    }
}