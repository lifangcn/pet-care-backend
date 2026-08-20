package pvt.mktech.petcare.chat.contentsearch;

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
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import pvt.mktech.petcare.chat.dto.SearchResult;
import pvt.mktech.petcare.sync.mapper.contentsearch.ContentSearchMapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostgresqlContentSearchIntegrationTest {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16"));
    private static final String OWNER = "content_owner", AI = "content_ai", CORE = "content_core";
    private static final String OWNER_PASSWORD = UUID.randomUUID().toString(), AI_PASSWORD = UUID.randomUUID().toString(), CORE_PASSWORD = UUID.randomUUID().toString();
    private static SqlSessionFactory factory;

    @BeforeAll
    static void setup() throws Exception {
        POSTGRES.start();
        try (Connection connection = connect(POSTGRES.getUsername(), POSTGRES.getPassword()); Statement statement = connection.createStatement()) {
            statement.execute("CREATE ROLE petcare_app NOLOGIN");
            statement.execute("CREATE ROLE " + OWNER + " LOGIN SUPERUSER PASSWORD '" + OWNER_PASSWORD + "'");
            statement.execute("CREATE ROLE " + AI + " LOGIN NOSUPERUSER PASSWORD '" + AI_PASSWORD + "'");
            statement.execute("CREATE ROLE " + CORE + " LOGIN NOSUPERUSER PASSWORD '" + CORE_PASSWORD + "'");
        }
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), OWNER, OWNER_PASSWORD).locations("classpath:db/migration")
                .schemas("petcare").defaultSchema("petcare").createSchemas(true).load().migrate();
        try (Connection connection = connect(OWNER, OWNER_PASSWORD); Statement statement = connection.createStatement()) {
            statement.execute("REVOKE ALL ON ALL TABLES IN SCHEMA petcare FROM PUBLIC, petcare_app");
            statement.execute("GRANT USAGE ON SCHEMA petcare TO " + AI + ", " + CORE);
            statement.execute("GRANT SELECT ON petcare.tb_post, petcare.tb_activity TO " + CORE);
            statement.execute("DROP INDEX petcare.idx_tb_post_audit_status, petcare.idx_tb_post_deleted, petcare.idx_tb_activity_audit_status, petcare.idx_tb_activity_deleted, petcare.idx_tb_activity_time_status");
            seed(statement);
        }
        FlexGlobalConfig.getDefaultConfig().setDbType(DbType.POSTGRE_SQL);
        FlexDataSource source = new FlexDataSource("ai", new PooledDataSource("org.postgresql.Driver", jdbcUrl(), AI, AI_PASSWORD), DbType.POSTGRE_SQL, true);
        source.addDataSource("core", new PooledDataSource("org.postgresql.Driver", jdbcUrl(), CORE, CORE_PASSWORD), DbType.POSTGRE_SQL, true);
        FlexConfiguration configuration = new FlexConfiguration();
        configuration.setEnvironment(new Environment("postgresql", new JdbcTransactionFactory(), source));
        configuration.addMapper(ContentSearchMapper.class);
        factory = new SqlSessionFactoryBuilder().build(configuration);
    }

    @AfterAll
    static void teardown() {
        if (POSTGRES.isRunning()) POSTGRES.stop();
    }

    @Test
    void routesSearchesOnlyThroughCoreDatasource() throws Exception {
        assertThatThrownBy(() -> DataSourceKey.use("ai", (Supplier<List<?>>) () -> {
            try (SqlSession session = factory.openSession()) {
                return session.getMapper(ContentSearchMapper.class).searchPosts("猫咪护理", "%猫咪护理%", "猫咪护理%", 5);
            }
        })).hasMessageContaining("permission denied");
        assertThat(DataSourceKey.get()).isNull();
        List<SearchResult> results = search(service -> service.searchPosts("猫咪护理", 5));
        assertThat(results).extracting(SearchResult::getTitle).contains("猫咪护理指南");
        assertThat(DataSourceKey.get()).isNull();
    }

    @Test
    void searchesPostsWithRankingFiltersLimitsEscapingAndMetadata() {
        List<SearchResult> posts = search(service -> service.searchPosts("猫咪护理", 99));
        assertThat(posts.getFirst().getTitle()).isEqualTo("猫咪护理指南");
        assertThat(posts).allSatisfy(result -> assertThat(result.getScore()).isBetween(0d, 1d));
        assertThat(posts).extracting(SearchResult::getTitle).doesNotContain("禁用猫咪护理", "待审核猫咪护理", "已删除猫咪护理", "关联猫咪护理");
        assertThat(search(service -> service.searchPosts("稳定排序", 5))).extracting(SearchResult::getTitle).containsExactly("稳定排序乙", "稳定排序甲");
        assertThat(search(service -> service.searchPosts("猫", 1))).hasSize(1);
        assertThat(search(service -> service.searchPosts("狗狗", 1))).hasSize(1);
        assertThat(search(service -> service.searchPosts(" ", 1))).isEmpty();
        assertThat(search(service -> service.searchPosts("猫".repeat(65), 1))).isEmpty();
        assertThat(search(service -> service.searchPosts("百分比100%_\\路径", 1))).singleElement().extracting(SearchResult::getTitle).isEqualTo("百分比100%_\\路径");
        SearchResult result = search(service -> service.searchPosts("空元数据", 1)).getFirst();
        assertThat(result.getMetadata()).containsKeys("id", "user_id", "post_type", "location_address", "price_range", "like_count", "rating_avg", "created_at");
        assertThat(result.getMetadata().get("location_address")).isNull();
    }

    @Test
    void searchesActivitiesWithStatusAndShanghaiTimeBounds() {
        assertThat(search(service -> service.searchActivities("标题命中", 5, null, null))).extracting(SearchResult::getTitle).containsExactly("标题命中活动");
        assertThat(search(service -> service.searchActivities("描述命中", 5, null, null))).extracting(SearchResult::getTitle).containsExactly("描述活动");
        assertThat(search(service -> service.searchActivities("地址命中", 5, null, null))).extracting(SearchResult::getTitle).containsExactly("地址活动");
        assertThat(search(service -> service.searchActivities("排除活动", 5, null, null))).isEmpty();
        assertThat(search(service -> service.searchActivities("时间边界", 5, "2026-02-22T00:00:00Z", "2026-02-22T12:00:00Z")))
                .extracting(SearchResult::getTitle).containsExactlyInAnyOrder("时间边界开始", "时间边界结束");
        assertThat(search(service -> service.searchActivities("标题命中", 1, null, null)).getFirst().getMetadata())
                .containsKeys("id", "user_id", "activity_time", "address", "status", "created_at");
    }

    @Test
    void usesV8PartialTrigramIndexesForThreeCharacterQueries() throws Exception {
        try (Connection connection = connect(OWNER, OWNER_PASSWORD); Statement statement = connection.createStatement()) {
            statement.execute("SET enable_seqscan = off");
            statement.execute("SET enable_indexscan = off");
            assertThat(explain(statement, "SELECT id FROM petcare.tb_post WHERE lower(coalesce(title, '') || ' ' || coalesce(content, '')) LIKE '%canine%' AND enabled = 1 AND audit_status = 'APPROVED' AND is_deleted = false AND post_type IN ('PRODUCT', 'SERVICE', 'LOCATION', 'DAILY')")).contains("idx_tb_post_content_search_trgm");
            assertThat(explain(statement, "SELECT id FROM petcare.tb_activity WHERE lower(coalesce(title, '') || ' ' || coalesce(description, '') || ' ' || coalesce(address, '')) LIKE '%meetup%' AND audit_status = 'APPROVED' AND is_deleted = false AND status IN ('RECRUITING', 'ONGOING')")).contains("idx_tb_activity_content_search_trgm");
        }
    }

    private static String explain(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery("EXPLAIN " + sql)) {
            StringBuilder plan = new StringBuilder();
            while (result.next()) plan.append(result.getString(1)).append('\n');
            return plan.toString();
        }
    }

    private static List<SearchResult> search(java.util.function.Function<ContentSearchService, List<SearchResult>> action) {
        return DataSourceKey.use("core", (Supplier<List<SearchResult>>) () -> {
            try (SqlSession session = factory.openSession()) {
                ContentSearchService service = new ContentSearchService(new PostgresqlContentSearchBackend(session.getMapper(ContentSearchMapper.class)));
                ReflectionTestUtils.setField(service, "backendName", "postgresql");
                ReflectionTestUtils.setField(service, "businessZone", "Asia/Shanghai");
                return action.apply(service);
            }
        });
    }

    private static void seed(Statement statement) throws Exception {
        post(statement, "猫咪护理指南", null, "DAILY", 1, "APPROVED", false, "2026-02-20 10:00:00"); post(statement, "经验分享", "猫咪护理步骤", "DAILY", 1, "APPROVED", false, "2026-02-20 10:00:00"); post(statement, "canine search", null, "DAILY", 1, "APPROVED", false, "2026-02-20 10:00:00");
        post(statement, "稳定排序甲", "稳定排序", "DAILY", 1, "APPROVED", false, "2026-02-20 11:00:00"); post(statement, "稳定排序乙", "稳定排序", "DAILY", 1, "APPROVED", false, "2026-02-20 11:00:00");
        post(statement, "猫", null, "DAILY", 1, "APPROVED", false, "2026-02-20 12:00:00"); post(statement, "狗狗", null, "DAILY", 1, "APPROVED", false, "2026-02-20 12:00:00"); post(statement, "百分比100%_\\路径", null, "DAILY", 1, "APPROVED", false, "2026-02-20 12:00:00"); post(statement, null, "空元数据", "DAILY", 1, "APPROVED", false, "2026-02-20 12:00:00");
        post(statement, "禁用猫咪护理", null, "DAILY", 0, "APPROVED", false, "2026-02-20 12:00:00"); post(statement, "待审核猫咪护理", null, "DAILY", 1, "PENDING", false, "2026-02-20 12:00:00"); post(statement, "已删除猫咪护理", null, "DAILY", 1, "APPROVED", true, "2026-02-20 12:00:00"); post(statement, "关联猫咪护理", null, "ACTIVITY_JOIN", 1, "APPROVED", false, "2026-02-20 12:00:00");
        activity(statement, "标题命中活动", null, null, "RECRUITING", "APPROVED", false, "2026-02-22 09:00:00"); activity(statement, "描述活动", "描述命中内容", null, "ONGOING", "APPROVED", false, "2026-02-22 09:00:00"); activity(statement, "地址活动", null, "地址命中地点", "RECRUITING", "APPROVED", false, "2026-02-22 09:00:00"); activity(statement, "meetup index", null, null, "RECRUITING", "APPROVED", false, "2026-02-22 09:00:00");
        activity(statement, "待审排除活动", null, null, "RECRUITING", "PENDING", false, "2026-02-22 09:00:00"); activity(statement, "结束排除活动", null, null, "ENDED", "APPROVED", false, "2026-02-22 09:00:00"); activity(statement, "删除排除活动", null, null, "RECRUITING", "APPROVED", true, "2026-02-22 09:00:00");
        activity(statement, "时间边界开始", null, null, "RECRUITING", "APPROVED", false, "2026-02-22 08:00:00"); activity(statement, "时间边界结束", null, null, "RECRUITING", "APPROVED", false, "2026-02-22 20:00:00"); activity(statement, "时间边界超出", null, null, "RECRUITING", "APPROVED", false, "2026-02-22 20:00:01");
    }

    private static void post(Statement s, String title, String content, String type, int enabled, String audit, boolean deleted, String created) throws Exception { s.execute("INSERT INTO petcare.tb_post (user_id,title,content,post_type,enabled,audit_status,is_deleted,created_at) VALUES (1," + literal(title) + "," + literal(content) + ",'" + type + "'," + enabled + ",'" + audit + "'," + deleted + ",TIMESTAMP '" + created + "')"); }
    private static void activity(Statement s, String title, String description, String address, String status, String audit, boolean deleted, String time) throws Exception { s.execute("INSERT INTO petcare.tb_activity (user_id,title,description,address,activity_type,activity_time,status,audit_status,is_deleted) VALUES (1," + literal(title) + "," + literal(description) + "," + literal(address) + ",'ONLINE',TIMESTAMP '" + time + "','" + status + "','" + audit + "'," + deleted + ")"); }
    private static String literal(String value) { return value == null ? "NULL" : "'" + value.replace("'", "''") + "'"; }
    private static Connection connect(String user, String password) throws Exception { return DriverManager.getConnection(jdbcUrl(), user, password); }
    private static String jdbcUrl() { return POSTGRES.getJdbcUrl() + "&currentSchema=petcare"; }
}
