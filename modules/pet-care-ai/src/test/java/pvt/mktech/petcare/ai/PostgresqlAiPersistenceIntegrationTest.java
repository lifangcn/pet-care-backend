package pvt.mktech.petcare.ai;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.datasource.FlexDataSource;
import com.mybatisflex.core.datasource.DataSourceKey;
import com.mybatisflex.core.dialect.DbType;
import com.mybatisflex.core.mybatis.FlexConfiguration;
import com.mybatisflex.core.query.QueryWrapper;
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
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import pvt.mktech.petcare.knowledge.entity.KnowledgeDocument;
import pvt.mktech.petcare.knowledge.entity.codelist.ProcessingStatusOfKnowledgeDocument;
import pvt.mktech.petcare.knowledge.mapper.KnowledgeDocumentMapper;
import pvt.mktech.petcare.sync.mapper.core.ActivityMapper;
import pvt.mktech.petcare.sync.mapper.core.PostMapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static pvt.mktech.petcare.knowledge.entity.table.KnowledgeDocumentTableDef.DOCUMENT;

class PostgresqlAiPersistenceIntegrationTest {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16"));
    private static final String OWNER = "ai_migration_owner";
    private static final String AI = "ai_login";
    private static final String CORE = "core_reader";
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
            statement.execute("REVOKE ALL ON ALL SEQUENCES IN SCHEMA petcare FROM petcare_app");
            statement.execute("GRANT USAGE ON SCHEMA petcare TO " + AI + ", " + CORE);
            statement.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON petcare.tb_knowledge_document TO " + AI);
            statement.execute("GRANT USAGE, SELECT ON SEQUENCE petcare.tb_knowledge_document_id_seq TO " + AI);
            statement.execute("GRANT SELECT ON petcare.tb_post, petcare.tb_activity TO " + CORE);
            statement.execute("INSERT INTO petcare.tb_post (user_id, title, post_type) VALUES (1, 'core post', 'DAILY')");
            statement.execute("INSERT INTO petcare.tb_activity (user_id, title, activity_type, activity_time) VALUES (1, 'core activity', 'ONLINE', CURRENT_TIMESTAMP)");
        }
        FlexGlobalConfig.getDefaultConfig().setDbType(DbType.POSTGRE_SQL);
        PooledDataSource aiDataSource = new PooledDataSource("org.postgresql.Driver", POSTGRES.getJdbcUrl() + "&currentSchema=petcare", AI, AI_PASSWORD);
        PooledDataSource coreDataSource = new PooledDataSource("org.postgresql.Driver", POSTGRES.getJdbcUrl() + "&currentSchema=petcare", CORE, CORE_PASSWORD);
        FlexDataSource dataSource = new FlexDataSource("ai", aiDataSource, DbType.POSTGRE_SQL, true);
        dataSource.addDataSource("core", coreDataSource, DbType.POSTGRE_SQL, true);
        FlexConfiguration configuration = new FlexConfiguration();
        configuration.setUseGeneratedKeys(true);
        configuration.setEnvironment(new Environment("postgresql", new JdbcTransactionFactory(), dataSource));
        FlexGlobalConfig.getDefaultConfig().setConfiguration(configuration);
        configuration.addMapper(KnowledgeDocumentMapper.class);
        configuration.addMapper(PostMapper.class);
        configuration.addMapper(ActivityMapper.class);
        factory = new SqlSessionFactoryBuilder().build(configuration);
        FlexGlobalConfig.getDefaultConfig().setSqlSessionFactory(factory);
    }

    @AfterAll static void teardown() { POSTGRES.stop(); }

    @Test
    void persistsAiDataAndRoutesCoreReads() throws Exception {
        assertConfiguration();
        try (Connection ai = connect(AI, AI_PASSWORD); Connection core = connect(CORE, CORE_PASSWORD)) {
            assertThat(schema(ai)).isEqualTo("petcare");
            assertThat(schema(core)).isEqualTo("petcare");
        }
        try (SqlSession session = factory.openSession(false)) {
            KnowledgeDocumentMapper documents = session.getMapper(KnowledgeDocumentMapper.class);
            KnowledgeDocument document = new KnowledgeDocument();
            document.upload("document", "file://document", "txt", 1L);
            assertThat(documents.insertSelective(document)).isEqualTo(1);
            KnowledgeDocument stored = documents.selectOneByQuery(QueryWrapper.create().where(DOCUMENT.NAME.eq("document")));
            assertThat(stored.getId()).isNotNull();
            assertThat(stored.getIsDeleted()).isFalse();
            assertThat(stored.getProcessingStatus()).isEqualTo(ProcessingStatusOfKnowledgeDocument.PROCESSING);
            LocalDateTime updatedAt = stored.getUpdatedAt();
            Thread.sleep(10);
            stored.updateProcessSuccess(1);
            documents.update(stored);
            KnowledgeDocument updated = documents.selectOneById(stored.getId());
            assertThat(updated.getProcessingStatus()).isEqualTo(ProcessingStatusOfKnowledgeDocument.COMPLETED);
            assertThat(updated.getUpdatedAt()).isAfter(updatedAt);
            String corePost = DataSourceKey.use("core", (Supplier<String>) () -> readCorePost());
            String coreActivity = DataSourceKey.use("core", (Supplier<String>) () -> readCoreActivity());
            assertThat(corePost).isEqualTo("core post");
            assertThat(coreActivity).isEqualTo("core activity");
            assertThat(DataSourceKey.get()).isNull();
            session.rollback();
        }
        try (SqlSession session = factory.openSession()) {
            assertThat(session.getMapper(KnowledgeDocumentMapper.class).selectCountByQuery(
                    QueryWrapper.create().where(DOCUMENT.NAME.eq("document")))).isZero();
        }
        try (Connection owner = connect(OWNER, OWNER_PASSWORD); var statement = owner.createStatement(); var result = statement.executeQuery("SELECT count(*) FROM pg_tables WHERE schemaname = 'petcare' AND tablename LIKE '%vector_store%'")) {
            result.next(); assertThat(result.getInt(1)).isZero();
        }
    }

    private static void assertConfiguration() throws Exception {
        YamlPropertiesFactoryBean loader = new YamlPropertiesFactoryBean();
        loader.setResources(new ClassPathResource("application-postgresql.yml"));
        Properties properties = loader.getObject();
        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("spring.flyway.enabled")).isEqualTo("false");
        assertThat(properties.getProperty("mybatis-flex.datasource.ai.url")).startsWith("jdbc:postgresql:");
        assertThat(properties.getProperty("mybatis-flex.datasource.core.url")).startsWith("jdbc:postgresql:");
        assertThat(properties.getProperty("mybatis-flex.datasource.ai.driver-class-name")).isEqualTo("org.postgresql.Driver");
        assertThat(properties.getProperty("mybatis-flex.datasource.core.driver-class-name")).isEqualTo("org.postgresql.Driver");
        assertThat(properties.getProperty("mybatis-flex.datasource.ai.password")).isEqualTo("${POSTGRES_PASSWORD}");
        assertThat(properties.getProperty("mybatis-flex.datasource.core.password")).isEqualTo("${POSTGRES_PASSWORD}");
    }
    private static String schema(Connection connection) throws Exception { try (var s = connection.createStatement(); var r = s.executeQuery("SELECT current_schema()")) { r.next(); return r.getString(1); } }
    private static Connection connect(String user, String password) throws Exception { return DriverManager.getConnection(POSTGRES.getJdbcUrl() + "&currentSchema=petcare", user, password); }
    private static String readCorePost() { try (SqlSession session = factory.openSession()) { return session.getMapper(PostMapper.class).selectListByQuery(QueryWrapper.create()).getFirst().getTitle(); } }
    private static String readCoreActivity() { try (SqlSession session = factory.openSession()) { return session.getMapper(ActivityMapper.class).selectListByQuery(QueryWrapper.create()).getFirst().getTitle(); } }
}
