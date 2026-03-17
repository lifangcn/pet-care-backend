package pvt.mktech.petcare.observability.config;

import com.knuddels.jtokkit.api.EncodingType;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pvt.mktech.petcare.observability.advisor.ObservabilityAdvisor;
import pvt.mktech.petcare.observability.appender.StructuredLogAppender;

/**
 * 可观测性自动配置
 * 职责：统一管理可观测性相关 Bean 的创建，通过配置属性控制功能开关
 *
 * @description: 自动配置类，当 observability.enabled=true 时创建所有相关 Bean
 * @date: 2026-03-06
 * @author Michael Li
 */
@Configuration
@EnableConfigurationProperties(ObservabilityProperties.class)
@ConditionalOnProperty(prefix = "observability", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ObservabilityAutoConfiguration {

    /**
     * 创建 Token 估算器
     * 使用 CL100K_BASE 编码（GPT-4/GPT-3.5-Turbo 兼容）
     * 注意：智谱 AI 使用自有的 token 计算，这里仅作为近似估算
     */
    @Bean
    public TokenCountEstimator tokenCountEstimator() {
        return new JTokkitTokenCountEstimator(EncodingType.CL100K_BASE);
    }

    /**
     * 创建结构化日志追加器
     * 负责将链路追踪日志异步写入 ES
     */
    @Bean
    public StructuredLogAppender structuredLogAppender(ElasticsearchClient elasticsearchClient,
                                                        TokenCountEstimator tokenCountEstimator,
                                                        ObservabilityProperties properties) {
        return new StructuredLogAppender(elasticsearchClient, tokenCountEstimator, properties.getEs().getIndexName());
    }

    /**
     * 创建可观测性 Advisor
     * 追踪 AI 对话链路，记录请求、响应、RAG、Tool Calling 等信息
     */
    @Bean
    public ObservabilityAdvisor observabilityAdvisor(StructuredLogAppender structuredLogAppender) {
        return new ObservabilityAdvisor(structuredLogAppender);
    }
}
