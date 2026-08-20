package pvt.mktech.petcare.observability.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.datasource.DataSourceKey;
import com.mybatisflex.core.datasource.FlexDataSource;
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
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import pvt.mktech.petcare.agent.context.AgentExecutionRecord;
import pvt.mktech.petcare.agent.repository.PostgresqlAgentExecutionStore;
import pvt.mktech.petcare.agent.telemetry.mapper.AgentExecutionTelemetryMapper;
import pvt.mktech.petcare.observability.config.TelemetryProperties;
import pvt.mktech.petcare.observability.dto.ChatTraceDocument;
import pvt.mktech.petcare.observability.mapper.ChatTraceMapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostgresqlTelemetryIntegrationTest {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16"));
    private static final String OWNER = "telemetry_owner";
    private static final String AI = "ai_login";
    private static final String CORE = "core_reader";
    private static final String OWNER_PASSWORD = UUID.randomUUID().toString();
    private static final String AI_PASSWORD = UUID.randomUUID().toString();
    private static final String CORE_PASSWORD = UUID.randomUUID().toString();
    private static SqlSessionFactory factory;

    @BeforeAll
    static void setUp() throws Exception {
        POSTGRES.start();
        try (Connection connection = connect(POSTGRES.getUsername(), POSTGRES.getPassword()); var statement = connection.createStatement()) {
            statement.execute("CREATE ROLE petcare_app NOLOGIN");
            statement.execute("CREATE ROLE " + OWNER + " LOGIN SUPERUSER PASSWORD '" + OWNER_PASSWORD + "'");
            statement.execute("CREATE ROLE " + AI + " LOGIN NOSUPERUSER PASSWORD '" + AI_PASSWORD + "'");
            statement.execute("CREATE ROLE " + CORE + " LOGIN NOSUPERUSER PASSWORD '" + CORE_PASSWORD + "'");
            statement.execute("GRANT petcare_app TO " + AI);
        }
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), OWNER, OWNER_PASSWORD).locations("filesystem:" + migrationsPath())
                .schemas("petcare").defaultSchema("petcare").createSchemas(true).load().migrate();
        FlexDataSource sources = new FlexDataSource("ai", new PooledDataSource("org.postgresql.Driver", jdbcUrl(), AI, AI_PASSWORD), DbType.POSTGRE_SQL, true);
        sources.addDataSource("core", new PooledDataSource("org.postgresql.Driver", jdbcUrl(), CORE, CORE_PASSWORD), DbType.POSTGRE_SQL, true);
        FlexConfiguration configuration = new FlexConfiguration();
        configuration.setEnvironment(new Environment("postgresql", new JdbcTransactionFactory(), sources));
        configuration.addMapper(ChatTraceMapper.class);
        configuration.addMapper(AgentExecutionTelemetryMapper.class);
        factory = new SqlSessionFactoryBuilder().build(configuration);
        FlexGlobalConfig.getDefaultConfig().setConfiguration(configuration);
        FlexGlobalConfig.getDefaultConfig().setSqlSessionFactory(factory);
    }

    @AfterAll
    static void tearDown() {
        POSTGRES.stop();
    }

    @Test
    void persistsOverwritesAndCleansExpiredTelemetryWithRoleIsolation() throws Exception {
        String traceId = UUID.randomUUID().toString();
        String executionId = UUID.randomUUID().toString();
        Instant initial = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        inAiSession(session -> {
            TelemetryProperties properties = new TelemetryProperties();
            properties.setRetentionDays(2);
            PostgresqlChatTraceStore chatStore = new PostgresqlChatTraceStore(session.getMapper(ChatTraceMapper.class), new ObjectMapper(), properties);
            ChatTraceDocument trace = trace(traceId, initial);
            chatStore.save(trace);
            trace.setDurationMs(99);
            trace.setTimestamp(initial.plusSeconds(60));
            chatStore.save(trace);
            PostgresqlAgentExecutionStore agentStore = new PostgresqlAgentExecutionStore(session.getMapper(AgentExecutionTelemetryMapper.class), new ObjectMapper());
            setRetentionDays(agentStore, 2);
            agentStore.save(execution(executionId, initial));
            agentStore.save(execution(executionId, initial.plusSeconds(60)));
            session.commit();
        });
        try (Connection owner = connect(OWNER, OWNER_PASSWORD); var statement = owner.createStatement()) {
            var rows = statement.executeQuery("SELECT count(*), max(duration_ms), max(expires_at), (array_agg(request_data::text))[1], (array_agg(response_data::text))[1], (array_agg(rag_data::text))[1], (array_agg(tool_calls::text))[1], (array_agg(error_data::text))[1], (array_agg(metadata::text))[1] FROM petcare.chat_trace WHERE trace_id = '" + traceId + "'::uuid");
            rows.next();
            assertThat(rows.getInt(1)).isEqualTo(1);
            assertThat(rows.getInt(2)).isEqualTo(99);
            assertThat(rows.getTimestamp(3).toInstant()).isEqualTo(initial.plusSeconds(60).plusSeconds(172800));
            assertThat(rows.getString(4)).isEqualTo("{}");
            assertThat(rows.getString(5)).isEqualTo("{}");
            assertThat(rows.getString(6)).isNull();
            assertThat(rows.getString(7)).isEqualTo("[]");
            assertThat(rows.getString(8)).isNull();
            assertThat(rows.getString(9)).isEqualTo("{}");
            rows = statement.executeQuery("SELECT count(*), max(created_at), max(expires_at), (array_agg(steps::text))[1], max(agent_type), max(query), bool_and(success), max(total_steps), max(tool_calls), max(total_duration_ms) FROM petcare.agent_execution WHERE execution_id = '" + executionId + "'::uuid");
            rows.next();
            assertThat(rows.getInt(1)).isEqualTo(1);
            assertThat(rows.getTimestamp(2).toInstant()).isEqualTo(initial.plusSeconds(60));
            assertThat(rows.getTimestamp(3).toInstant()).isEqualTo(initial.plusSeconds(60).plusSeconds(172800));
            assertThat(rows.getString(4)).isEqualTo("[]");
            assertThat(rows.getString(5)).isEqualTo("REACT");
            assertThat(rows.getString(6)).isEqualTo("query");
            assertThat(rows.getBoolean(7)).isTrue();
            assertThat(rows.getInt(8)).isZero();
            assertThat(rows.getInt(9)).isZero();
            assertThat(rows.getLong(10)).isEqualTo(1L);
            statement.execute("INSERT INTO petcare.chat_trace (trace_id, occurred_at, expires_at) VALUES ('" + UUID.randomUUID() + "', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - INTERVAL '1 second')");
            statement.execute("INSERT INTO petcare.chat_trace (trace_id, occurred_at, expires_at) VALUES ('" + UUID.randomUUID() + "', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '1 day')");
        }
        inAiSession(session -> {
            ChatTraceMapper mapper = session.getMapper(ChatTraceMapper.class);
            assertThat(mapper.deleteExpiredBatch(1)).isEqualTo(1);
            assertThat(mapper.deleteExpiredBatch(1)).isZero();
            session.commit();
        });
        try (Connection owner = connect(OWNER, OWNER_PASSWORD); var statement = owner.createStatement();
             var rows = statement.executeQuery("SELECT count(*) FROM petcare.chat_trace WHERE expires_at > CURRENT_TIMESTAMP")) {
            rows.next();
            assertThat(rows.getInt(1)).isGreaterThanOrEqualTo(1);
        }
        assertThatThrownBy(() -> inSession("core", session -> session.getMapper(ChatTraceMapper.class).deleteExpiredBatch(1)))
                .hasRootCauseInstanceOf(java.sql.SQLException.class);
        assertThat(DataSourceKey.get()).isNull();
    }

    private static ChatTraceDocument trace(String traceId, Instant timestamp) {
        ChatTraceDocument trace = new ChatTraceDocument();
        trace.setTraceId(traceId);
        trace.setTimestamp(timestamp);
        trace.setDurationMs(1);
        return trace;
    }

    private static AgentExecutionRecord execution(String id, Instant createdAt) {
        return AgentExecutionRecord.builder().executionId(id).agentType("REACT").query("query").steps(null)
                .success(true).totalSteps(0).toolCalls(0).totalDurationMs(1).createdAt(createdAt).build();
    }

    private static void setRetentionDays(PostgresqlAgentExecutionStore store, long value) {
        org.springframework.test.util.ReflectionTestUtils.setField(store, "retentionDays", value);
    }

    private static void inAiSession(Consumer<SqlSession> action) {
        inSession("ai", action);
    }

    private static void inSession(String key, Consumer<SqlSession> action) {
        DataSourceKey.use(key, (Supplier<Void>) () -> {
            try (SqlSession session = factory.openSession(false)) {
                action.accept(session);
                return null;
            }
        });
    }

    private static String jdbcUrl() { return POSTGRES.getJdbcUrl() + "&currentSchema=petcare"; }
    private static Connection connect(String user, String password) throws Exception { return DriverManager.getConnection(jdbcUrl(), user, password); }
    private static Path migrationsPath() {
        Path rootPath = Path.of(System.getProperty("user.dir"), "modules", "pet-care-core", "src", "main", "resources", "db", "migration");
        if (Files.isDirectory(rootPath)) {
            return rootPath;
        }
        return Path.of(System.getProperty("user.dir"), "..", "pet-care-core", "src", "main", "resources", "db", "migration").normalize();
    }
}
