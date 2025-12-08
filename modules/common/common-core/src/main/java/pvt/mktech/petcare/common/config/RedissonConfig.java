package pvt.mktech.petcare.common.config;

import lombok.extern.slf4j.Slf4j;

/**
 * Redisson 配置类
 */
//@Slf4j
//@Configuration
public class RedissonConfig {
    
    /*@Value("${spring.redis.host:localhost}")
    private String redisHost;
    
    @Value("${spring.redis.port:6379}")
    private String redisPort;
    
    @Value("${spring.redis.password:123456}")
    private String redisPassword;
    
    @Value("${spring.redis.database:0}")
    private int redisDatabase;
    
    @Value("${spring.redis.timeout:3000}")
    private int redisTimeout;
    
//    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        try {
            Config config = new Config();
            
            String redisAddress = String.format("redis://%s:%s", redisHost, redisPort);
            
            SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress(redisAddress)
                .setDatabase(redisDatabase)
                .setTimeout(redisTimeout)
                .setConnectionPoolSize(64)
                .setConnectionMinimumIdleSize(24)
                .setSubscriptionConnectionPoolSize(50)
                .setSubscriptionConnectionMinimumIdleSize(1);
            
            if (redisPassword != null && !redisPassword.isEmpty()) {
                singleServerConfig.setPassword(redisPassword);
            }
            
            // 序列化配置
            config.setCodec(new JsonJacksonCodec());
            
            RedissonClient redisson = Redisson.create(config);
            log.info("Redisson 客户端初始化成功");
            return redisson;

        } catch (RedisConnectionFailureException e) {
            log.error("无法连接到Redis服务器: {}", e.getMessage());
            throw new RuntimeException("Redisson 初始化失败: 无法连接到Redis服务器", e);
        } catch (Exception e) {
            log.error("Redisson 客户端初始化失败", e);
            throw new RuntimeException("Redisson 初始化失败", e);
        }
    }*/
}