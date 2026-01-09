package pvt.mktech.petcare.common.snowflake;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(SnowflakeIdGenerator.class)
@ConditionalOnProperty(prefix = "snowflake", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SnowflakeAutoConfiguration {
    static {
        log.info("初始化雪花算法 ID 生成器");
    }
}
