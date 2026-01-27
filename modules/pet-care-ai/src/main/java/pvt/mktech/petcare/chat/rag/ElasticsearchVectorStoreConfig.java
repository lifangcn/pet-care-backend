package pvt.mktech.petcare.chat.rag;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * {@code @description}: 向量数据库配置类 Elasticsearch
 * {@code @date}: 2026/1/16 14:38
 *
 * @author Michael
 */
@Slf4j
@Configuration
@EnableAspectJAutoProxy
public class ElasticsearchVectorStoreConfig {

    // 向量数据库手动配置
    @Bean
    public VectorStore elasticsearchVectorStore(RestClient restClient, EmbeddingModel dashScopeEmbeddingModel) {
        ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
        options.setIndexName("pet-care-vector-store-index");    // Optional: defaults to "spring-ai-document-index"
        options.setDimensions(1024);             // Optional: defaults to model dimensions or 1536

        return ElasticsearchVectorStore.builder(restClient, dashScopeEmbeddingModel)
                .options(options)                     // Optional: use custom options
                .initializeSchema(true)               // Optional: defaults to false
                .build();
    }
}
