package pvt.mktech.petcare.common.util;

import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import static pvt.mktech.petcare.common.constant.CommonConstant.RATE_LIMIT_KEY;

@Component
public class RateLimiterUtil {

    private final RedissonClient redissonClient;

    public RateLimiterUtil(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 尝试获取令牌
     *
     * @param key  限流key
     * @param rate 速率（每秒多少请求）
     * @return 是否获取成功
     */
    public boolean tryAcquire(String key, int rate) {
        RRateLimiter limiter = redissonClient.getRateLimiter(key);

        // 初始化限流器：每秒钟rate个令牌
        limiter.trySetRate(RateType.PER_CLIENT, rate, 1, RateIntervalUnit.SECONDS);

        return limiter.tryAcquire(1);
    }

    /**
     * 尝试获取令牌（带超时时间）
     */
    public boolean tryAcquire(String key, int rate, long timeout, java.util.concurrent.TimeUnit unit) {
        RRateLimiter limiter = redissonClient.getRateLimiter(key);
        limiter.trySetRate(RateType.PER_CLIENT, rate, 1, RateIntervalUnit.SECONDS);

        return limiter.tryAcquire(1, timeout, unit);
    }

    /**
     * 获取API限流器
     */
    public RRateLimiter getApiLimiter(String apiPath, int rate) {
        String key = RATE_LIMIT_KEY + apiPath;
        RRateLimiter limiter = redissonClient.getRateLimiter(key);
        limiter.trySetRate(RateType.PER_CLIENT, rate, 1, RateIntervalUnit.SECONDS);
        return limiter;
    }
}