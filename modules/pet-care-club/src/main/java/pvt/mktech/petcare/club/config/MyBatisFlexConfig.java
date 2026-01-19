package pvt.mktech.petcare.club.config;

import com.mybatisflex.core.audit.AuditManager;
import com.mybatisflex.core.audit.MessageCollector;
import com.mybatisflex.spring.boot.MyBatisFlexCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;

/**
 * MyBatis-Flex 配置类。
 */
@org.springframework.context.annotation.Configuration
public class MyBatisFlexConfig {

    private static final Logger logger = LoggerFactory.getLogger(MyBatisFlexConfig.class);

    @Bean
    public MyBatisFlexCustomizer myBatisFlexCustomizer() {
        return globalConfig -> {
            globalConfig.setPrintBanner(false);
            AuditManager.setAuditEnable(true);
            AuditManager.setMessageCollector(message ->
                    logger.info("SQL Audit: {} - {}ms", message.getFullSql(), message.getElapsedTime()));
        };
    }
}
