package pvt.mktech.petcare.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.sync.service.IndexAdminService;

/**
 * {@code @description}: Elasticsearch 索引自动初始化
 * <p>
 * 应用启动时自动创建所需的索引，可通过配置禁用：es.index.auto-init=false
 * </p>
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "es.index.auto-init", havingValue = "true", matchIfMissing = true)
public class IndexInitializationRunner implements ApplicationRunner {

    private final IndexAdminService indexAdminService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            indexAdminService.initAllIndices();
        } catch (Exception e) {
            log.error("索引初始化失败，请检查 Elasticsearch 连接", e);
        }
    }
}
