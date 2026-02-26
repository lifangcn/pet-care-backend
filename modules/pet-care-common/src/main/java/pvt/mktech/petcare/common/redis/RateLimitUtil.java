package pvt.mktech.petcare.common.redis;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;

/**
 * {@code @description}: 限流工具类（基于令牌桶算法）
 * {@code @date}: 2026/02/26
 *
 * @author Michael Li
 */
@Slf4j
public record RateLimitUtil(RedissonClient redissonClient) {

    /**
     * 尝试获取许可（令牌桶模式）
     *
     * @param key         限流key
     * @param rate        每秒生成令牌数
     * @param rateInterval 间隔时间（秒）
     * @return 是否获取成功
     */
    public boolean tryAcquire(String key, long rate, long rateInterval) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        // 如果限流器不存在则创建
        if (!rateLimiter.isExists()) {
            rateLimiter.trySetRate(RateType.OVERALL, rate, rateInterval, RateIntervalUnit.SECONDS);
        }
        return rateLimiter.tryAcquire(1);
    }

    /**
     * 尝试获取许可（指定令牌数）
     *
     * @param key         限流key
     * @param permits     需要的令牌数
     * @param rate        每秒生成令牌数
     * @param rateInterval 间隔时间（秒）
     * @return 是否获取成功
     */
    public boolean tryAcquire(String key, long permits, long rate, long rateInterval) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        // 如果限流器不存在则创建
        if (!rateLimiter.isExists()) {
            rateLimiter.trySetRate(RateType.OVERALL, rate, rateInterval, RateIntervalUnit.SECONDS);
        }
        return rateLimiter.tryAcquire(permits);
    }

    /**
     * 设置限流速率
     *
     * @param key         限流key
     * @param rate        每秒生成令牌数
     * @param rateInterval 间隔时间（秒）
     */
    public void setRate(String key, long rate, long rateInterval) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.setRate(RateType.OVERALL, rate, rateInterval, RateIntervalUnit.SECONDS);
    }

    /**
     * 删除限流器
     *
     * @param key 限流key
     */
    public void delete(String key) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.delete();
    }
}
