package pvt.mktech.petcare.common.redis;

import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static pvt.mktech.petcare.common.constant.CommonConstant.*;

/**
 * {@code @description}: 自定义分布式ID生成器
 * {@code @date}: 2026/1/29 15:00
 *
 * @author Michael Li
 */
public record DistributedIdGenerator(RedissonClient redissonClient) {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy:MM:dd");

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