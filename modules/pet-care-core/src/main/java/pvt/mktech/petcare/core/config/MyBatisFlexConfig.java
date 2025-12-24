package pvt.mktech.petcare.core.config;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.audit.AuditManager;
import com.mybatisflex.spring.boot.MyBatisFlexCustomizer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Configuration
public class MyBatisFlexConfig implements MyBatisFlexCustomizer {
    @Override
    public void customize(FlexGlobalConfig flexGlobalConfig) {
        // 开启审计功能
        AuditManager.setAuditEnable(true);

        // 设置 SQL 审计收集器
        AuditManager.setMessageCollector(auditMessage ->
                log.info("SQL Audit: {} - {}ms", auditMessage.getFullSql(), auditMessage.getElapsedTime()));
    }
}
