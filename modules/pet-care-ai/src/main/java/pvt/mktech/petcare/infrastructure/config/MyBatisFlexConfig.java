package pvt.mktech.petcare.infrastructure.config;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.audit.AuditManager;
import com.mybatisflex.core.mybatis.FlexConfiguration;
import com.mybatisflex.spring.boot.ConfigurationCustomizer;
import com.mybatisflex.spring.boot.MyBatisFlexCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;


@Configuration
public class MyBatisFlexConfig implements ConfigurationCustomizer, MyBatisFlexCustomizer {
    private static final Logger logger = LoggerFactory.getLogger("mybatis-flex-sql");

    @Override
    public void customize(FlexConfiguration flexConfiguration) {
//        flexConfiguration.setLogImpl(StdOutImpl.class);
        flexConfiguration.setMapUnderscoreToCamelCase(true);
        flexConfiguration.setCacheEnabled(true);
        // 设置 SQL 审计收集器
        AuditManager.setAuditEnable(true);
        AuditManager.setMessageCollector(auditMessage ->
                logger.info("SQL Audit: {} - {}ms", auditMessage.getFullSql(), auditMessage.getElapsedTime()));
    }

    @Override
    public void customize(FlexGlobalConfig flexGlobalConfig) {
        flexGlobalConfig.setPrintBanner(false);
    }
}
