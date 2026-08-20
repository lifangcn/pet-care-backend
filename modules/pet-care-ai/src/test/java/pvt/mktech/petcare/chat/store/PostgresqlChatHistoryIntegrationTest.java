package pvt.mktech.petcare.chat.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.datasource.FlexDataSource;
import com.mybatisflex.core.datasource.DataSourceKey;
import com.mybatisflex.core.dialect.DbType;
import com.mybatisflex.core.mybatis.FlexConfiguration;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import pvt.mktech.petcare.chat.mapper.ChatHistoryMapper;
import pvt.mktech.petcare.entity.ChatMessageDocument;
import pvt.mktech.petcare.infrastructure.config.ChatMemoryProperties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Exercises the V1-V10 schema using separate migration, ai, and core roles. */
class PostgresqlChatHistoryIntegrationTest {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16"));
    private static final String OWNER = "chat_migration_owner";
    private static final String AI = "chat_ai_login";
    private static final String CORE = "chat_core_login";
    private static final String OWNER_PASSWORD = UUID.randomUUID().toString();
    private static final String AI_PASSWORD = UUID.randomUUID().toString();
    private static final String CORE_PASSWORD = UUID.randomUUID().toString();
    private static SqlSessionFactory factory;

    @BeforeAll
    static void setup() throws Exception {
        POSTGRES.start();
        try (Connection connection = connect(POSTGRES.getUsername(), POSTGRES.getPassword()); var statement = connection.createStatement()) {
            statement.execute("CREATE ROLE petcare_app NOLOGIN");
            statement.execute("CREATE ROLE " + OWNER + " LOGIN SUPERUSER PASSWORD '" + OWNER_PASSWORD + "'");
            statement.execute("CREATE ROLE " + AI + " LOGIN NOSUPERUSER PASSWORD '" + AI_PASSWORD + "'");
            statement.execute("CREATE ROLE " + CORE + " LOGIN NOSUPERUSER PASSWORD '" + CORE_PASSWORD + "'");
            statement.execute("GRANT petcare_app TO " + AI);
        }
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), OWNER, OWNER_PASSWORD).locations("classpath:db/migration")
                .schemas("petcare").defaultSchema("petcare").createSchemas(true).load().migrate();
        try (Connection connection = connect(OWNER, OWNER_PASSWORD); var statement = connection.createStatement()) {
            statement.execute("REVOKE ALL ON ALL TABLES IN SCHEMA petcare FROM petcare_app");
            statement.execute("GRANT USAGE ON SCHEMA petcare TO " + AI + ", " + CORE);
            statement.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON petcare.chat_session, petcare.chat_message TO " + AI);
        }
        FlexGlobalConfig.getDefaultConfig().setDbType(DbType.POSTGRE_SQL);
        FlexDataSource dataSource = new FlexDataSource("ai", new PooledDataSource("org.postgresql.Driver", jdbcUrl(), AI, AI_PASSWORD), DbType.POSTGRE_SQL, true);
        dataSource.addDataSource("core", new PooledDataSource("org.postgresql.Driver", jdbcUrl(), CORE, CORE_PASSWORD), DbType.POSTGRE_SQL, true);
        FlexConfiguration configuration = new FlexConfiguration();
        configuration.setEnvironment(new Environment("postgresql", new JdbcTransactionFactory(), dataSource));
        configuration.addMapper(ChatHistoryMapper.class);
        factory = new SqlSessionFactoryBuilder().build(configuration);
    }

    @AfterAll
    static void teardown() { POSTGRES.stop(); }

    @BeforeEach
    void reset() throws Exception {
        try (Connection connection = connect(OWNER, OWNER_PASSWORD); var statement = connection.createStatement()) {
            statement.execute("TRUNCATE petcare.chat_session CASCADE");
        }
    }

    @Test
    void persistsMetadataFiltersExpiryAndUsesOwnerScopedCascade() {
        String session = "s" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        withStore(store -> { store.ensureSession(1L, session, "新对话"); return null; });
        Instant created = Instant.now().minus(1, ChronoUnit.MINUTES);
        ChatMessageDocument message = message(101L, 1L, session, created, created.plus(2, ChronoUnit.DAYS), "USER");
        withStore(store -> { store.saveMessages(List.of(message)); return null; });
        assertThat(this.<List<ChatMessageDocument>>withStore(store -> store.getSessionHistory(1L, session, 10))).singleElement().satisfies(stored ->
                assertThat(stored.getMetadata()).isEqualTo(Map.of("nested", Map.of("count", 2), "tags", List.of("a", "b"))));
        assertThat(this.<Long>withStore(store -> store.countMessages(1L, session))).isEqualTo(1);

        assertThat(this.<Boolean>withStore(store -> store.updateDefaultSessionName(1L, session, "标题"))).isTrue();
        Instant updatedAt = sessionUpdatedAt(1L, session);
        assertThat(this.<Boolean>withStore(store -> store.updateDefaultSessionName(1L, session, "不会更新"))).isFalse();
        assertThat(sessionUpdatedAt(1L, session)).isEqualTo(updatedAt);

        String otherSession = "o" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        withStore(store -> { store.ensureSession(2L, otherSession, "新对话"); return null; });
        assertThatThrownBy(() -> withStore(store -> { store.saveMessages(List.of(message(102L, 2L, session, created, created.plus(1, ChronoUnit.DAYS), "USER"))); return null; }))
                .isInstanceOf(RuntimeException.class);
        assertThat(this.<Long>withStore(store -> store.deleteSession(1L, session))).isEqualTo(1);
        assertThat(this.<Long>withStore(store -> store.countMessages(1L, session))).isZero();
        assertThat(this.<java.util.Optional<pvt.mktech.petcare.chat.dto.response.SessionItem>>withStore(store -> store.getSession(2L, otherSession))).isPresent();
    }

    @Test
    void retriesDoNotTouchAndSemanticSearchHonorsOwnerExpiryHistoryAndScore() throws Exception {
        String session = "s" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        withStore(store -> { store.ensureSession(1L, session, "新对话"); return null; });
        Instant created = Instant.now().minus(2, ChronoUnit.HOURS);
        ChatMessageDocument first = message(201L, 1L, session, created, created.plus(3, ChronoUnit.DAYS), "USER");
        withStore(store -> { store.saveMessages(List.of(first)); return null; });
        Instant touched = sessionUpdatedAt(1L, session);
        withStore(store -> { store.saveMessages(List.of(first)); return null; });
        assertThat(sessionUpdatedAt(1L, session)).isEqualTo(touched);

        ChatMessageDocument newer = message(202L, 1L, session, Instant.now().plus(1, ChronoUnit.HOURS), Instant.now().plus(5, ChronoUnit.DAYS), "USER");
        withStore(store -> { store.saveMessages(List.of(newer)); return null; });
        assertThat(sessionUpdatedAt(1L, session)).isEqualTo(newer.getCreatedAt());
        assertThat(this.<List<ChatMessageDocument>>withStore(store -> store.semanticSearch(1L, vector(1.0f), 2, 0.9, 1)).stream().map(ChatMessageDocument::getId).toList()).containsExactly(201L, 202L);
        assertThat(this.<List<ChatMessageDocument>>withStore(store -> store.semanticSearch(2L, vector(1.0f), 2, 0.9, 1))).isEmpty();

        expireSession(1L, session);
        assertThat(this.<List<ChatMessageDocument>>withStore(store -> store.getSessionHistory(1L, session, 10))).isEmpty();
        assertThat(this.<Long>withStore(store -> store.countSessions(1L))).isZero();
        assertThatThrownBy(() -> withStore(store -> store.semanticSearch(null, vector(1.0f), 1, 0.5, 1))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cleanupIsBoundedAndCoreCannotUseAiTables() throws Exception {
        String first = "s" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String second = "s" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        withStore(store -> { store.ensureSession(1L, first, "新对话"); return null; });
        withStore(store -> { store.ensureSession(1L, second, "新对话"); return null; });
        Instant expired = Instant.now().minus(2, ChronoUnit.DAYS);
        withStore(store -> { store.saveMessages(List.of(message(301L, 1L, first, expired, expired.plus(1, ChronoUnit.HOURS), "USER"))); return null; });
        expireSession(1L, first); expireSession(1L, second);
        assertThat(this.<Integer>withStore(store -> store.deleteExpiredBatch(1))).isEqualTo(2);
        assertThat(this.<Integer>withStore(store -> store.deleteExpiredBatch(1))).isEqualTo(1);
        assertThatThrownBy(() -> DataSourceKey.use("core", (Supplier<Void>) () -> withMapper(mapper -> { mapper.ensureSession(9L, "core-denied", "新对话", 1); return null; })))
                .isInstanceOf(RuntimeException.class);
        assertThat(DataSourceKey.get()).isNull();
    }

    private <T> T withStore(Function<PostgresqlChatHistoryStore, T> action) {
        return DataSourceKey.use("ai", (Supplier<T>) () -> withMapper(mapper -> {
        ChatMemoryProperties properties = new ChatMemoryProperties();
        properties.getHistory().setRetentionDays(30);
            return action.apply(new PostgresqlChatHistoryStore(mapper, properties, new ObjectMapper()));
        }));
    }

    private <T> T withMapper(Function<ChatHistoryMapper, T> action) {
        try (SqlSession sqlSession = factory.openSession(true)) {
            return action.apply(sqlSession.getMapper(ChatHistoryMapper.class));
        }
    }

    private Instant sessionUpdatedAt(Long userId, String sessionId) {
        try (SqlSession sqlSession = factory.openSession()) {
            return sqlSession.getMapper(ChatHistoryMapper.class).getSession(userId, sessionId).updatedAt();
        }
    }

    private void expireSession(Long userId, String sessionId) throws Exception {
        try (Connection connection = connect(OWNER, OWNER_PASSWORD); var statement = connection.prepareStatement("UPDATE petcare.chat_session SET created_at=CURRENT_TIMESTAMP-INTERVAL '3 hour', updated_at=CURRENT_TIMESTAMP-INTERVAL '2 hour', expires_at=CURRENT_TIMESTAMP-INTERVAL '1 hour' WHERE user_id=? AND session_id=?")) {
            statement.setLong(1, userId); statement.setString(2, sessionId); statement.executeUpdate();
        }
    }

    private static ChatMessageDocument message(Long id, Long userId, String sessionId, Instant createdAt, Instant expiresAt, String role) {
        ChatMessageDocument message = new ChatMessageDocument();
        message.setId(id); message.setUserId(userId); message.setSessionId(sessionId); message.setConversationId("conversation-" + sessionId);
        message.setRole(role); message.setContent("content"); message.setEmbedding(vector(1.0f));
        message.setMetadata(Map.of("nested", Map.of("count", 2), "tags", List.of("a", "b"))); message.setCreatedAt(createdAt); message.setExpiresAt(expiresAt);
        return message;
    }

    private static List<Float> vector(float first) {
        List<Float> vector = new java.util.ArrayList<>(1024);
        vector.add(first); for (int i = 1; i < 1024; i++) vector.add(0.0f);
        return vector;
    }

    private static String jdbcUrl() { return POSTGRES.getJdbcUrl() + "&currentSchema=petcare"; }
    private static Connection connect(String user, String password) throws Exception { return DriverManager.getConnection(jdbcUrl(), user, password); }
}
