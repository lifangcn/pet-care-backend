package pvt.mktech.petcare.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import pvt.mktech.petcare.common.redis.DistributedLock;
import pvt.mktech.petcare.common.redis.IdGenerator;

public class RedissonConfig {

    @Bean
    @ConditionalOnMissingBean(RedissonClient.class)
    @ConditionalOnProperty(prefix = "spring.data.redis", name = "host")
    public RedissonClient redissonClient(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.password:123456}") String password,
            @Value("${spring.data.redis.database:15}") int database) {
        Config config = new Config();
        String address = "redis://" + host + ":" + port;
        if (StringUtils.hasText(password)) {
            config.useSingleServer()
                    .setAddress(address)
                    .setPassword(password)
                    .setDatabase(database);
        } else {
            config.useSingleServer()
                    .setAddress(address)
                    .setDatabase(database);
        }
        return Redisson.create(config);
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.data.redis", name = "host")
    @ConditionalOnMissingBean(DistributedLock.class)
    public DistributedLock distributedLock(RedissonClient redissonClient) {
        return new DistributedLock(redissonClient);
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.data.redis", name = "host")
    @ConditionalOnMissingBean(IdGenerator.class)
    public IdGenerator idGenerator(RedissonClient redissonClient) {
        return new IdGenerator(redissonClient);
    }
}

