package pvt.mktech.petcare.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private String redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();

        // 单节点模式（生产环境可用集群模式）
        String address = String.format("redis://%s:%s", redisHost, redisPort);
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisDatabase)
                .setPassword(redisPassword.isEmpty() ? null : redisPassword)
                .setConnectionPoolSize(64) // 连接池大小
                .setConnectionMinimumIdleSize(10) // 最小空闲连接数
                .setIdleConnectionTimeout(10000) // 连接空闲超时时间
                .setConnectTimeout(10000) // 连接超时时间
                .setTimeout(3000) // 操作超时时间
                .setRetryAttempts(3) // 重试次数
                .setRetryInterval(1500); // 重试间隔
        
        return Redisson.create(config);
    }
}