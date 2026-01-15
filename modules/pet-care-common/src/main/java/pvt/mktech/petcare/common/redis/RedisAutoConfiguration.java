package pvt.mktech.petcare.common.redis;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@Slf4j
@AutoConfiguration
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnProperty(prefix = "redisson", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisAutoConfiguration {

    @Bean
    public RedisUtil redisCacheUtil(RedissonClient redissonClient) {
        log.info("初始化 Redis 缓存工具");
        return new RedisUtil(redissonClient);
    }

    @Bean
    public RedissonLockUtil redissonLockUtil(RedissonClient redissonClient) {
        log.info("初始化 Redisson 分布式锁工具");
        return new RedissonLockUtil(redissonClient);
    }

    @Bean
    public DistributedIdGenerator distributedIdGenerator(RedissonClient redissonClient) {
        log.info("初始化分布式ID生成器");
        return new DistributedIdGenerator(redissonClient);
    }
}
