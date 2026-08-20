package pvt.mktech.petcare.observability.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pvt.mktech.petcare.observability.mapper.ChatTraceMapper;
import pvt.mktech.petcare.observability.store.ChatTraceStore;
import pvt.mktech.petcare.observability.store.ElasticsearchChatTraceStore;
import pvt.mktech.petcare.observability.store.PostgresqlChatTraceStore;

/** Selects the configured chat trace storage backend. */
@Configuration
@EnableConfigurationProperties(TelemetryProperties.class)
public class ChatTraceStoreConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "petcare.telemetry", name = "store", havingValue = "elasticsearch", matchIfMissing = true)
    public ChatTraceStore elasticsearchChatTraceStore(ElasticsearchClient elasticsearchClient,
                                                       ObservabilityProperties properties) {
        return new ElasticsearchChatTraceStore(elasticsearchClient, properties.getEs().getIndexName());
    }

    @Bean
    @ConditionalOnProperty(prefix = "petcare.telemetry", name = "store", havingValue = "postgresql")
    public ChatTraceStore postgresqlChatTraceStore(ChatTraceMapper chatTraceMapper, ObjectMapper objectMapper,
                                                    TelemetryProperties properties) {
        return new PostgresqlChatTraceStore(chatTraceMapper, objectMapper, properties);
    }
}
