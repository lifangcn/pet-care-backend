package pvt.mktech.petcare.ai.rag;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

/**
 * {@code @description}:
 * {@code @date}: 2026/1/16 14:38
 *
 * @author Michael
 */
@Slf4j
@Configuration
@EnableAspectJAutoProxy
public class PgVectorVectorStoreConfig {

    @Value("${spring.ai.pgvector.datasource.url}")
    private String jdbcUrl;
    @Value("${spring.ai.pgvector.datasource.username}")
    private String username;
    @Value("${spring.ai.pgvector.datasource.password}")
    private String password;
    @Value("${spring.ai.pgvector.datasource.driver-class-name}")
    private String driverClassName;

    // 配置数据库连接
    @Bean
    public JdbcTemplate postgresqlJdbcTemplate() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        return new JdbcTemplate(dataSource);
    }

    // 向量数据库手动配置： pgVector
    @Bean
    public VectorStore customPgVectorStore(@Qualifier("postgresqlJdbcTemplate") JdbcTemplate postgresqlJdbcTemplate,
                                           EmbeddingModel dashScopeEmbeddingModel) {
        return PgVectorStore.builder(postgresqlJdbcTemplate, dashScopeEmbeddingModel)
                .dimensions(1024)                    // text-embedding-v3: 1024 Optional: defaults to model dimensions or 1536
                .distanceType(COSINE_DISTANCE)       // Optional: defaults to COSINE_DISTANCE
                .indexType(HNSW)                     // Optional: defaults to HNSW
                .initializeSchema(true)              // Optional: defaults to false
                .schemaName("public")                // Optional: defaults to "public"
                .vectorTableName("vector_store")     // Optional: defaults to "vector_store"
                .maxDocumentBatchSize(10000)         // Optional: defaults to 10000
                .build();
    }
}
