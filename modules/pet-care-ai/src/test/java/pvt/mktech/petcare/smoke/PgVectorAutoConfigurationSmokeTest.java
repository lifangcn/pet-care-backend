package pvt.mktech.petcare.smoke;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class PgVectorAutoConfigurationSmokeTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16"));

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    JdbcTemplateAutoConfiguration.class,
                    PgVectorStoreAutoConfiguration.class))
            .withUserConfiguration(FakeEmbeddingConfiguration.class);

    @Test
    void autoConfigurationBindsPropertiesAndPersistsVectors() {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker is required for the PGVector smoke test");
        POSTGRES.start();
        try {
            contextRunner
                .withPropertyValues(
                        "spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                        "spring.datasource.username=" + POSTGRES.getUsername(),
                        "spring.datasource.password=" + POSTGRES.getPassword(),
                        "spring.ai.vectorstore.type=pgvector",
                        "spring.ai.vectorstore.pgvector.table-name=vector_store_smoke",
                        "spring.ai.vectorstore.pgvector.dimensions=1024",
                        "spring.ai.vectorstore.pgvector.distance-type=COSINE_DISTANCE",
                        // 阶段 3 将由 Flyway 管理 schema；此隔离 smoke 显式开启以验证 1.0.1 的建表兼容性。
                        "spring.ai.vectorstore.pgvector.initialize-schema=true",
                        "spring.ai.vectorstore.pgvector.schema-validation=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(PgVectorStore.class);
                    assertThat(context).getBeans(VectorStore.class).hasSize(1);

                    PgVectorStoreProperties properties = context.getBean(PgVectorStoreProperties.class);
                    PgVectorStore vectorStore = context.getBean(PgVectorStore.class);
                    assertThat(properties.getDimensions()).isEqualTo(1024);
                    assertThat(vectorStore.getDistanceType()).isEqualTo(PgVectorStore.PgDistanceType.COSINE_DISTANCE);

                    Document document = new Document("pgvector smoke document");
                    vectorStore.add(List.of(document));
                    assertThat(vectorStore.similaritySearch(SearchRequest.builder()
                            .query("pgvector smoke query")
                            .topK(1)
                            .similarityThreshold(0.0)
                            .build())).extracting(Document::getId).contains(document.getId());

                    vectorStore.delete(List.of(document.getId()));
                    assertThat(vectorStore.similaritySearch("pgvector smoke query")).isEmpty();
                    });
        } finally {
            POSTGRES.stop();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class FakeEmbeddingConfiguration {

        @Bean
        EmbeddingModel embeddingModel() {
            return new EmbeddingModel() {
                @Override
                public EmbeddingResponse call(EmbeddingRequest request) {
                    List<Embedding> embeddings = IntStream.range(0, request.getInstructions().size())
                            .mapToObj(index -> new Embedding(vector(), index))
                            .toList();
                    return new EmbeddingResponse(embeddings);
                }

                @Override
                public float[] embed(Document document) {
                    return vector();
                }

                @Override
                public int dimensions() {
                    return 1024;
                }

                private float[] vector() {
                    float[] vector = new float[1024];
                    vector[0] = 1.0f;
                    return vector;
                }
            };
        }
    }
}
