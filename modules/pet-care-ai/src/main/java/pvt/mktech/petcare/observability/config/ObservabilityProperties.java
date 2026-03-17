package pvt.mktech.petcare.observability.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 可观测性配置属性
 * 职责：从 application.yml 读取 observability.* 配置
 *
 * @description: 可观测性功能的配置属性类
 * @date: 2026-03-06
 * @author Michael Li
 */
@Data
@ConfigurationProperties(prefix = "observability")
public class ObservabilityProperties {

    /**
     * 是否启用可观测性
     */
    private boolean enabled = true;

    /**
     * Elasticsearch 配置
     */
    private EsConfig es = new EsConfig();

    @Data
    public static class EsConfig {
        /**
         * ES 索引名称
         */
        private String indexName = "chat_trace";

        /**
         * 是否自动初始化索引
         */
        private boolean autoInit = true;
    }
}
