package pvt.mktech.petcare.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import pvt.mktech.petcare.sync.constants.EsIndexConstants;

import static pvt.mktech.petcare.sync.constants.SyncConstants.KNOWLEDGE_DOCUMENT_INDEX;

/**
 * {@code @description}: Elasticsearch 向量存储配置类
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Slf4j
@Configuration
@EnableAspectJAutoProxy
public class ElasticsearchVectorStoreConfig {

    /**
     * 配置 Spring AI Elasticsearch VectorStore
     * 用于知识库文档的向量存储和检索
     */
    @Bean
    public VectorStore elasticsearchVectorStore(RestClient restClient, EmbeddingModel dashScopeEmbeddingModel) {
        ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
        options.setIndexName(KNOWLEDGE_DOCUMENT_INDEX);
        options.setDimensions(1024);  // DashScope text-embedding-v3 维度

        return ElasticsearchVectorStore.builder(restClient, dashScopeEmbeddingModel)
                .options(options)
                .initializeSchema(true)  // 自动创建索引映射
                .batchingStrategy(new TokenCountBatchingStrategy())
                .build();
    }
}
