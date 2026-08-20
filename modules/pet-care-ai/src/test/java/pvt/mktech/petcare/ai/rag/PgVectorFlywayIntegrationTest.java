package pvt.mktech.petcare.ai.rag;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class PgVectorFlywayIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16"));
    private static final String MIGRATION_OWNER = "ai_vector_migration_owner";
    private static final String APPLICATION_LOGIN = "ai_vector_application";
    private static final String OWNER_PASSWORD = UUID.randomUUID().toString();
    private static final String APPLICATION_PASSWORD = UUID.randomUUID().toString();

    @BeforeAll
    static void setUpDatabase() throws Exception {
        POSTGRES.start();
        try (Connection connection = connect(POSTGRES.getUsername(), POSTGRES.getPassword()); var statement = connection.createStatement()) {
            statement.execute("CREATE ROLE petcare_app NOLOGIN");
            statement.execute("CREATE ROLE " + MIGRATION_OWNER + " LOGIN SUPERUSER PASSWORD '" + OWNER_PASSWORD + "'");
            statement.execute("CREATE ROLE " + APPLICATION_LOGIN + " LOGIN NOSUPERUSER PASSWORD '" + APPLICATION_PASSWORD + "'");
            statement.execute("GRANT petcare_app TO " + APPLICATION_LOGIN);
        }
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), MIGRATION_OWNER, OWNER_PASSWORD)
                .locations("classpath:db/migration").schemas("petcare").defaultSchema("petcare")
                .createSchemas(true).load().migrate();
    }

    @AfterAll
    static void tearDownDatabase() {
        POSTGRES.stop();
    }

    @Test
    void usesFlywayManagedPgVectorStoreWithApplicationRole() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
                        JdbcTemplateAutoConfiguration.class, PgVectorStoreAutoConfiguration.class))
                .withUserConfiguration(FakeEmbeddingConfiguration.class)
                .withPropertyValues(
                        "spring.datasource.url=" + POSTGRES.getJdbcUrl() + "&currentSchema=petcare",
                        "spring.datasource.username=" + APPLICATION_LOGIN,
                        "spring.datasource.password=" + APPLICATION_PASSWORD,
                        "spring.ai.vectorstore.type=pgvector",
                        "spring.ai.vectorstore.pgvector.schema-name=petcare",
                        "spring.ai.vectorstore.pgvector.table-name=vector_store",
                        "spring.ai.vectorstore.pgvector.dimensions=1024",
                        "spring.ai.vectorstore.pgvector.distance-type=COSINE_DISTANCE",
                        "spring.ai.vectorstore.pgvector.index-type=HNSW",
                        "spring.ai.vectorstore.pgvector.initialize-schema=false",
                        "spring.ai.vectorstore.pgvector.schema-validation=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(PgVectorStore.class);
                    assertThat(context).getBeans(VectorStore.class).hasSize(1);
                    JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
                    assertThat(jdbc.queryForObject("SELECT current_user", String.class)).isEqualTo(APPLICATION_LOGIN);
                    assertThat(jdbc.queryForObject("SELECT count(*) FROM petcare.flyway_schema_history WHERE version = '7' AND success", Integer.class)).isEqualTo(1);
                    assertThat(jdbc.queryForObject("SELECT has_table_privilege(current_user, 'petcare.vector_store', 'INSERT')", Boolean.class)).isTrue();
                    assertThat(jdbc.queryForObject("SELECT has_schema_privilege(current_user, 'petcare', 'CREATE')", Boolean.class)).isFalse();

                    VectorStore store = context.getBean(VectorStore.class);
                    String firstId = "00000000-0000-0000-0000-000000000001";
                    String secondId = "00000000-0000-0000-0000-000000000002";
                    Document first = new Document(firstId, "puppy vaccination schedule", Map.of("document_id", "first", "filename", "first.md"));
                    Document second = new Document(secondId, "kitten nutrition guide", Map.of("document_id", "second", "filename", "second.md"));
                    store.add(List.of(first, second));

                    List<Document> found = store.similaritySearch(SearchRequest.builder().query("puppy vaccination").topK(10)
                            .similarityThreshold(0.0).build());
                    assertThat(found).extracting(Document::getId).contains(firstId, secondId);
                    assertThat(found).allSatisfy(document -> {
                        assertThat(document.getScore()).isNotNull();
                        assertThat(document.getMetadata()).containsKey("document_id");
                    });

                    store.delete("document_id == 'first'");
                    assertThat(store.similaritySearch(SearchRequest.builder().query("query").topK(10).similarityThreshold(0.0).build()))
                            .extracting(Document::getId).contains(secondId).doesNotContain(firstId);
                    store.delete(List.of(secondId));
                    assertThat(store.similaritySearch(SearchRequest.builder().query("query").topK(10).similarityThreshold(0.0).build()))
                            .extracting(Document::getId).doesNotContain(secondId);
                });
    }

    private static Connection connect(String user, String password) throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), user, password);
    }

    @Configuration(proxyBeanMethods = false)
    static class FakeEmbeddingConfiguration {
        @Bean
        EmbeddingModel embeddingModel() {
            return new EmbeddingModel() {
                @Override public EmbeddingResponse call(EmbeddingRequest request) {
                    return new EmbeddingResponse(IntStream.range(0, request.getInstructions().size())
                            .mapToObj(index -> new Embedding(vector(), index)).toList());
                }
                @Override public float[] embed(Document document) { return vector(); }
                @Override public int dimensions() { return 1024; }
                private float[] vector() { float[] vector = new float[1024]; vector[0] = 1.0f; return vector; }
            };
        }
    }
}
