package pvt.mktech.petcare.common.redis;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;

import java.time.Duration;

/**
 * {@code @description}: 限流工具类（基于令牌桶算法），使用注解形式实现，手动限流暂时弃用
 * <p>
 * 令牌桶参数说明：
 * <ul>
 *   <li>rate: 每个 rateInterval 时间内生成的令牌数</li>
 *   <li>rateInterval: 时间间隔（秒）</li>
 *   <li>例如：rate=1, rateInterval=6 表示每6秒生成1个令牌</li>
 * </ul>
 * {@code @date}: 2026/02/26
 *
 * @author Michael Li
 */
@Deprecated
@Slf4j
public record RateLimitUtil(RedissonClient redissonClient) {

    /**
     * 尝试获取许可（令牌桶模式）
     * <p>
     * 使用 setRate 替代 trySetRate，确保配置动态生效
     *
     * @param key         限流key
     * @param permits     需要的令牌数
     * @param rate        每个 rateInterval 时间内生成的令牌数
     * @param rateInterval 时间间隔（秒）
     * @return 是否获取成功
     */
    public boolean tryAcquire(String key, long permits, long rate, long rateInterval) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        // 使用 setRate 确保配置始终有效，如果已存在则覆盖
        rateLimiter.setRate(RateType.OVERALL, rate, Duration.ofSeconds(rateInterval));
        return rateLimiter.tryAcquire(permits);
    }

    /**
     * 尝试获取许可（单令牌）
     *
     * @param key         限流key
     * @param rate        每个 rateInterval 时间内生成的令牌数
     * @param rateInterval 时间间隔（秒）
     * @return 是否获取成功
     */
    public boolean tryAcquire(String key, long rate, long rateInterval) {
        return tryAcquire(key, 1, rate, rateInterval);
    }

    /**
     * 设置限流速率
     *
     * @param key         限流key
     * @param rate        每个 rateInterval 时间内生成的令牌数
     * @param rateInterval 时间间隔（秒）
     */
    public void setRate(String key, long rate, long rateInterval) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.setRate(RateType.OVERALL, rate, Duration.ofSeconds(rateInterval));
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
