package pvt.mktech.petcare.common.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(JwtUtil.class)
@ConditionalOnProperty(prefix = "jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JwtAutoConfiguration {
    static {
        log.info("初始化 JWT 工具");
    }
}
