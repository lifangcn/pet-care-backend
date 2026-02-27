package pvt.mktech.petcare.infrastructure.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

/**
 * {@code @description}: Elasticsearch 配置类
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Slf4j
@Configuration
public class ElasticsearchConfig {

    /**
     * 创建 Elasticsearch 传输层，配置支持 Java 8 日期时间类型序列化
     */
    @Bean
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient) {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
        // Instant 序列化为 ISO-8601 字符串，兼容 ES date 类型
        mapper.configOverride(Instant.class)
                .setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING));
        return new RestClientTransport(restClient, new JacksonJsonpMapper(mapper));
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }
}
