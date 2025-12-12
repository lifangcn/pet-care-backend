package pvt.mktech.petcare.common.redis;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 基于Redisson的ID生成器，支持生成带日期前缀的唯一ID和递增序列ID
 * <p>
 * 特点：
 * 1. generateId系列方法：生成包含日期信息的唯一ID，每天自动过期重置
 * 2. generateSequenceId系列方法：生成全局递增的序列ID，持久化存储
 */
@RequiredArgsConstructor
public class IdGenerator {

    private final RedissonClient redissonClient;
    private static final String ID_PREFIX = "id:generator:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy:MM:dd");

    /**
     * 开始时间戳2025-01-01 00:00:00
     */
    private static final long BEGIN_TIMESTAMP = 1735689600L;
    private static final int COUNT_BITS = 32;

    /**
     * 设计自增全局唯一ID结构：0(符号位)-00000000 00000000 00000000 0000000(31bit时间戳，以秒为单位，可用69年)-00000000 00000000 00000000 00000000(32位序列号)
     */
    public Long generateId(String businessKey) {
        // 1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowEpochSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowEpochSecond - BEGIN_TIMESTAMP;
        // 2.设置唯一ID key: 功能标识(id:generator:) + 业务标识(businessKey:) + 日期(yyyyMMdd)
        String date = now.format(DATE_FORMATTER);
        String idRedisKey = ID_PREFIX + businessKey + ":" + date;
        // 2.生成序列号
        long count = redissonClient.getAtomicLong(idRedisKey).incrementAndGet();
        // 3.拼接：时间戳左移32位，或运算填充序列号
        return timestamp << COUNT_BITS | count;
    }
}

