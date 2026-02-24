package pvt.mktech.petcare.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import pvt.mktech.petcare.common.thread.ThreadPoolManager;
import pvt.mktech.petcare.sync.constants.EsIndexConstants;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import static pvt.mktech.petcare.sync.constants.SyncConstants.KNOWLEDGE_DOCUMENT_INDEX;

/**
 * {@code @description}: Elasticsearch 向量存储配置类
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Slf4j
@Configuration
@EnableAspectJAutoProxy
@EnableAsync
public class ElasticsearchVectorStoreConfig {

    /**
     * 关键词元信息增强器
     * 复用实例提升性能
     */
    @Bean
    public KeywordMetadataEnricher keywordMetadataEnricher(ChatModel zhiPuAiChatModel) {
        return new KeywordMetadataEnricher(zhiPuAiChatModel, 5);
    }

    /**
     * 配置 Spring AI Elasticsearch VectorStore
     * 用于知识库文档的向量存储和检索
     */
    @Bean
    public VectorStore elasticsearchVectorStore(RestClient restClient, EmbeddingModel zhiPuAiEmbeddingModel) {
        ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
        options.setIndexName(KNOWLEDGE_DOCUMENT_INDEX);
        options.setDimensions(1024);  // 智谱 embedding-2 维度

        return ElasticsearchVectorStore.builder(restClient, zhiPuAiEmbeddingModel)
                .options(options)
                .initializeSchema(true)  // 自动创建索引映射
                .batchingStrategy(new TokenCountBatchingStrategy())
                .build();
    }

    /**
     * 向量处理线程池
     * 用于文档向量异步处理
     */
    @Bean("vectorProcessExecutor")
    public Executor vectorProcessExecutor(MeterRegistry meterRegistry) {
        ThreadPoolExecutor executor = ThreadPoolManager.createThreadPool("vector-process");
        ExecutorServiceMetrics.monitor(meterRegistry, executor, "vector-process");
        executor.prestartAllCoreThreads();
        return executor;
    }
}
