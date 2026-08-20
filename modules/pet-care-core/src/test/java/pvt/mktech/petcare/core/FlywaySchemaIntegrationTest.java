package pvt.mktech.petcare.core;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlywaySchemaIntegrationTest {

    private static final Set<String> BUSINESS_TABLES = Set.of(
            "tb_user", "tb_pet", "tb_health_record", "tb_reminder", "tb_reminder_execution",
            "tb_post", "tb_label", "tb_post_label", "tb_interaction", "tb_activity",
            "tb_knowledge_document", "tb_points_account", "tb_points_record", "tb_points_coupon_template",
            "tb_points_coupon", "tb_role", "tb_user_role");

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16"));
    private static final String MIGRATION_OWNER = "migration_owner";
    private static final String APPLICATION_LOGIN = "application_login";
    private static final String MIGRATION_OWNER_PASSWORD = UUID.randomUUID().toString();
    private static final String APPLICATION_LOGIN_PASSWORD = UUID.randomUUID().toString();

    @BeforeAll
    static void startPostgres() {
        POSTGRES.start();
        try (Connection connection = openConnection(POSTGRES.getUsername(), POSTGRES.getPassword())) {
            execute(connection, "CREATE ROLE petcare_app NOLOGIN");
            // pgvector installation requires elevated privileges, so this temporary test migration owner simulates deployment.
            execute(connection, "CREATE ROLE " + MIGRATION_OWNER + " LOGIN SUPERUSER PASSWORD '" + MIGRATION_OWNER_PASSWORD + "'");
            execute(connection, "CREATE ROLE " + APPLICATION_LOGIN + " LOGIN NOSUPERUSER PASSWORD '" + APPLICATION_LOGIN_PASSWORD + "'");
            execute(connection, "GRANT petcare_app TO " + APPLICATION_LOGIN);
            execute(connection, "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\" WITH SCHEMA public");
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to bootstrap integration-test database roles", exception);
        }
    }

    @AfterAll
    static void stopPostgres() {
        if (POSTGRES.isRunning()) {
            POSTGRES.stop();
        }
    }

    @Test
    void migrateCreatesThePostgreSqlBusinessSchemaIdempotently() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), MIGRATION_OWNER, MIGRATION_OWNER_PASSWORD)
                .locations("classpath:db/migration")
                .schemas("petcare")
                .defaultSchema("petcare")
                .createSchemas(true)
                .load();

        MigrateResult firstMigration = flyway.migrate();
        assertThat(firstMigration.success).isTrue();

        try (Connection connection = openConnection(MIGRATION_OWNER, MIGRATION_OWNER_PASSWORD)) {
            assertThat(queryForString(connection, "SELECT current_user")).isEqualTo(MIGRATION_OWNER);
            assertExtensions(connection);
            assertSearchPathExtensionSupport(connection);
            assertBusinessTables(connection);
            assertVectorStore(connection);
            assertIdentityPrimaryKeys(connection);
            assertConstraintsAndIndexes(connection);
            assertContentSearchIndexes(connection);
            assertJsonColumns(connection);
            assertColumnContracts(connection);
            assertNoForeignKeys(connection);
            assertReferenceData(connection);
            assertConstraintBehavior(connection);
            assertUpdatedAtTrigger(connection);
            assertFlywayHistory(connection);
        }
        assertApplicationPrivileges();

        MigrateResult secondMigration = flyway.migrate();
        assertThat(secondMigration.success).isTrue();
        assertThat(secondMigration.migrationsExecuted).isZero();

        try (Connection connection = openConnection(MIGRATION_OWNER, MIGRATION_OWNER_PASSWORD)) {
            assertThat(queryForInt(connection, """
                    SELECT count(*)
                    FROM petcare.tb_role
                    WHERE role_code IN ('admin', 'user')
                    """)).isEqualTo(2);
            assertThat(queryForInt(connection, """
                    SELECT count(*)
                    FROM petcare.tb_points_coupon_template
                    WHERE name = '新人注册券' AND source_type = 'NEWCOMER'
                    """)).isEqualTo(1);
        }
    }

    private void assertExtensions(Connection connection) throws SQLException {
        assertThat(queryForStrings(connection, """
                SELECT extension.extname || ':' || namespace.nspname
                FROM pg_extension extension
                JOIN pg_namespace namespace ON namespace.oid = extension.extnamespace
                WHERE namespace.nspname = 'petcare'
                  AND extension.extname IN ('vector', 'pg_trgm', 'hstore', 'uuid-ossp')
                """)).containsExactlyInAnyOrder("vector:petcare", "pg_trgm:petcare");
        assertThat(queryForString(connection, """
                SELECT namespace.nspname
                FROM pg_extension extension
                JOIN pg_namespace namespace ON namespace.oid = extension.extnamespace
                WHERE extension.extname = 'uuid-ossp'
                """)).isEqualTo("public");
    }

    private void assertApplicationPrivileges() throws SQLException {
        try (Connection connection = openConnection(APPLICATION_LOGIN, APPLICATION_LOGIN_PASSWORD)) {
            assertThat(queryForString(connection, "SELECT current_user")).isEqualTo(APPLICATION_LOGIN);
            assertThat(queryForString(connection, "SELECT current_user")).isNotEqualTo(MIGRATION_OWNER);
            assertThat(queryForInt(connection, "SELECT CASE WHEN has_schema_privilege(current_user, 'petcare', 'USAGE') THEN 1 ELSE 0 END"))
                    .isEqualTo(1);
            assertThat(queryForInt(connection, "SELECT CASE WHEN has_schema_privilege(current_user, 'petcare', 'CREATE') THEN 1 ELSE 0 END"))
                    .isZero();
            assertThat(queryForInt(connection, "SELECT CASE WHEN has_database_privilege(current_user, current_database(), 'CREATE') THEN 1 ELSE 0 END"))
                    .isZero();
            assertThat(queryForInt(connection, """
                    SELECT count(*)
                    WHERE has_table_privilege(current_user, 'petcare.vector_store', 'SELECT')
                      AND has_table_privilege(current_user, 'petcare.vector_store', 'INSERT')
                      AND has_table_privilege(current_user, 'petcare.vector_store', 'UPDATE')
                      AND has_table_privilege(current_user, 'petcare.vector_store', 'DELETE')
                    """)).isEqualTo(1);
            assertThat(queryForInt(connection, """
                    SELECT count(*)
                    WHERE has_table_privilege(current_user, 'petcare.tb_user', 'SELECT')
                      AND has_table_privilege(current_user, 'petcare.tb_user', 'INSERT')
                      AND has_table_privilege(current_user, 'petcare.tb_user', 'UPDATE')
                      AND has_table_privilege(current_user, 'petcare.tb_user', 'DELETE')
                    """)).isEqualTo(1);
            assertThat(queryForInt(connection, """
                    SELECT count(*)
                    WHERE has_sequence_privilege(current_user, 'petcare.tb_user_id_seq', 'USAGE')
                      AND has_sequence_privilege(current_user, 'petcare.tb_user_id_seq', 'SELECT')
                      AND has_function_privilege(current_user, 'petcare.touch_updated_at()', 'EXECUTE')
                    """)).isEqualTo(1);
            assertThat(queryForInt(connection, """
                    SELECT CASE WHEN rolsuper THEN 1 ELSE 0 END
                    FROM pg_roles
                    WHERE rolname = current_user
                    """)).isZero();

            execute(connection, "SET search_path TO petcare, public");
            assertThat(queryForInt(connection, "SELECT count(*) FROM tb_role")).isGreaterThanOrEqualTo(2);
            assertThat(queryForInt(connection, "SELECT count(*) FROM tb_post")).isZero();
            assertThat(queryForInt(connection, "SELECT count(*) FROM tb_activity")).isZero();
            long userId = insertApplicationUser(connection);
            assertThat(queryForInt(connection, "SELECT count(*) FROM tb_user WHERE id = " + userId)).isEqualTo(1);
            assertThat(executeUpdate(connection, "UPDATE tb_user SET nickname = 'app' WHERE id = " + userId)).isEqualTo(1);
            assertThat(executeUpdate(connection, "DELETE FROM tb_user WHERE id = " + userId)).isEqualTo(1);
            assertThat(queryForInt(connection, "SELECT nextval('petcare.tb_user_id_seq')"))
                    .isGreaterThan(0);
            UUID vectorStoreId = insertVectorStoreDocument(connection);
            assertThat(queryForInt(connection, "SELECT count(*) FROM vector_store WHERE id = '" + vectorStoreId + "'"))
                    .isEqualTo(1);
            assertThat(executeUpdate(connection, "UPDATE vector_store SET content = 'updated' WHERE id = '" + vectorStoreId + "'"))
                    .isEqualTo(1);
            assertThat(executeUpdate(connection, "DELETE FROM vector_store WHERE id = '" + vectorStoreId + "'"))
                    .isEqualTo(1);

            assertSqlRejected(connection, "CREATE TABLE petcare.application_denied (id bigint)");
            assertSqlRejected(connection, "CREATE INDEX application_denied_vector_store_idx ON petcare.vector_store (content)");
            assertSqlRejected(connection, "CREATE EXTENSION hstore");
            assertSqlRejected(connection, "CREATE SCHEMA application_denied_schema");
            assertSqlRejected(connection, "CREATE ROLE application_denied_role");
        }
    }

    private long insertApplicationUser(Connection connection) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO tb_user (username)
                VALUES (?)
                RETURNING id
                """)) {
            statement.setString(1, "application_" + UUID.randomUUID());
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong("id");
            }
        }
    }

    private UUID insertVectorStoreDocument(Connection connection) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO vector_store (content, metadata, embedding)
                VALUES (?, ?::json, array_fill(0::real, ARRAY[1024])::petcare.vector)
                RETURNING id
                """)) {
            statement.setString(1, "application vector document");
            statement.setString(2, "{\"source\":\"integration-test\"}");
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getObject("id", UUID.class);
            }
        }
    }

    private void assertSearchPathExtensionSupport(Connection connection) throws SQLException {
        execute(connection, "SET search_path TO petcare, public");
        assertThat(queryForInt(connection, "SELECT CASE WHEN '[1,2,3]'::vector IS NOT NULL THEN 1 ELSE 0 END"))
                .isEqualTo(1);
        assertThat(queryForInt(connection, "SELECT CASE WHEN similarity('abc', 'abd') > 0 THEN 1 ELSE 0 END"))
                .isEqualTo(1);
        assertThat(queryForInt(connection, "SELECT count(*) FROM tb_role")).isEqualTo(2);
    }

    private void assertBusinessTables(Connection connection) throws SQLException {
        Set<String> tables = queryForStrings(connection, """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'petcare'
                  AND table_name LIKE 'tb_%'
                """);
        assertThat(tables).isEqualTo(BUSINESS_TABLES);
    }

    private void assertVectorStore(Connection connection) throws SQLException {
        assertThat(queryForInt(connection, """
                SELECT count(*)
                FROM information_schema.tables
                WHERE table_schema = 'petcare' AND table_name = 'vector_store'
                """)).isEqualTo(1);
        assertThat(queryForInt(connection, """
                SELECT count(*)
                FROM information_schema.columns
                WHERE table_schema = 'petcare'
                  AND table_name = 'vector_store'
                  AND (
                      (column_name = 'id' AND data_type = 'uuid' AND column_default LIKE '%uuid_generate_v4%')
                      OR (column_name = 'content' AND data_type = 'text')
                      OR (column_name = 'metadata' AND data_type = 'json')
                  )
                """)).isEqualTo(2);
        assertThat(queryForString(connection, """
                SELECT pg_get_expr(default_expression.adbin, default_expression.adrelid)
                FROM pg_attribute attribute
                JOIN pg_class table_name ON table_name.oid = attribute.attrelid
                JOIN pg_namespace namespace ON namespace.oid = table_name.relnamespace
                JOIN pg_attrdef default_expression
                    ON default_expression.adrelid = attribute.attrelid
                   AND default_expression.adnum = attribute.attnum
                WHERE namespace.nspname = 'petcare'
                  AND table_name.relname = 'vector_store'
                  AND attribute.attname = 'id'
                """)).isEqualTo("gen_random_uuid()");
        assertThat(queryForInt(connection, """
                SELECT count(*)
                FROM pg_constraint table_constraint
                JOIN pg_class table_name ON table_name.oid = table_constraint.conrelid
                JOIN pg_namespace namespace ON namespace.oid = table_name.relnamespace
                WHERE namespace.nspname = 'petcare'
                  AND table_name.relname = 'vector_store'
                  AND table_constraint.contype = 'p'
                """)).isEqualTo(1);
        assertThat(queryForInt(connection, """
                SELECT count(*)
                FROM pg_attribute attribute
                JOIN pg_class table_name ON table_name.oid = attribute.attrelid
                JOIN pg_namespace namespace ON namespace.oid = table_name.relnamespace
                WHERE namespace.nspname = 'petcare'
                  AND table_name.relname = 'vector_store'
                  AND attribute.attname = 'embedding'
                  AND attribute.atttypid = 'petcare.vector'::regtype
                  AND attribute.atttypmod = 1024
                """)).isEqualTo(1);
        assertThat(queryForInt(connection, """
                SELECT count(*)
                FROM pg_index index
                JOIN pg_class index_name ON index_name.oid = index.indexrelid
                JOIN pg_class table_name ON table_name.oid = index.indrelid
                JOIN pg_namespace namespace ON namespace.oid = table_name.relnamespace
                JOIN pg_am access_method ON access_method.oid = index_name.relam
                WHERE namespace.nspname = 'petcare'
                  AND table_name.relname = 'vector_store'
                  AND index_name.relname = 'idx_vector_store_embedding_hnsw'
                  AND access_method.amname = 'hnsw'
                  AND pg_get_indexdef(index.indexrelid) LIKE '%vector_cosine_ops%'
                """)).isEqualTo(1);
    }

    private void assertIdentityPrimaryKeys(Connection connection) throws SQLException {
        assertThat(queryForInt(connection, """
                SELECT count(*)
                FROM information_schema.columns
                WHERE table_schema = 'petcare'
                  AND table_name LIKE 'tb_%'
                  AND column_name = 'id'
                  AND is_identity = 'YES'
                  AND data_type = 'bigint'
                """)).isEqualTo(17);
        assertThat(queryForInt(connection, """
                SELECT count(DISTINCT table_name)
                FROM information_schema.table_constraints
                WHERE table_schema = 'petcare'
                  AND table_name LIKE 'tb_%'
                  AND constraint_type = 'PRIMARY KEY'
                """)).isEqualTo(17);
    }

    private void assertConstraintsAndIndexes(Connection connection) throws SQLException {
        assertThat(queryForStrings(connection, """
                SELECT conname
                FROM pg_constraint
                WHERE connamespace = 'petcare'::regnamespace
                  AND conname IN (
                      'uk_tb_user_phone',
                      'ck_tb_reminder_execution_status',
                      'ck_tb_interaction_rating_value',
                      'ck_tb_points_account_available_points'
                  )
                """)).containsExactlyInAnyOrder(
                "uk_tb_user_phone",
                "ck_tb_reminder_execution_status",
                "ck_tb_interaction_rating_value",
                "ck_tb_points_account_available_points");
        assertThat(queryForInt(connection, """
                SELECT count(*)
                FROM pg_constraint
                WHERE connamespace = 'petcare'::regnamespace
                  AND conname IN ('uk_tb_user_username', 'uk_tb_role_role_code', 'uk_tb_label_name')
                """)).isZero();
        assertThat(queryForStrings(connection, """
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'petcare'
                  AND indexname IN ('uk_tb_user_username_ci', 'uk_tb_role_role_code_ci', 'uk_tb_label_name_ci')
                  AND indexdef LIKE '%lower(%'
                """)).containsExactlyInAnyOrder(
                "uk_tb_user_username_ci", "uk_tb_role_role_code_ci", "uk_tb_label_name_ci");
        assertThat(queryForStrings(connection, """
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'petcare'
                  AND indexname IN ('idx_tb_reminder_schedule_time', 'idx_tb_activity_time_status')
                """)).containsExactlyInAnyOrder("idx_tb_reminder_schedule_time", "idx_tb_activity_time_status");
    }

    private void assertJsonColumns(Connection connection) throws SQLException {
        assertThat(queryForStrings(connection, """
                SELECT table_name || '.' || column_name
                FROM information_schema.columns
                WHERE table_schema = 'petcare'
                  AND data_type = 'jsonb'
                """)).containsExactlyInAnyOrder(
                "tb_reminder.repeat_config",
                "tb_post.media_urls",
                "tb_activity.labels");
    }

    private void assertContentSearchIndexes(Connection connection) throws SQLException {
        assertThat(queryForStrings(connection, """
                SELECT table_name.relname || ':' || access_method.amname || ':' || operator_class.opcname
                FROM pg_index index
                JOIN pg_class index_name ON index_name.oid = index.indexrelid
                JOIN pg_class table_name ON table_name.oid = index.indrelid
                JOIN pg_namespace namespace ON namespace.oid = table_name.relnamespace
                JOIN pg_am access_method ON access_method.oid = index_name.relam
                JOIN pg_opclass operator_class ON operator_class.oid = index.indclass[0]
                JOIN pg_namespace operator_class_namespace ON operator_class_namespace.oid = operator_class.opcnamespace
                WHERE namespace.nspname = 'petcare'
                  AND operator_class_namespace.nspname = 'petcare'
                  AND index_name.relname IN ('idx_tb_post_content_search_trgm', 'idx_tb_activity_content_search_trgm')
                """)).containsExactlyInAnyOrder(
                "tb_post:gin:gin_trgm_ops", "tb_activity:gin:gin_trgm_ops");
        assertThat(queryForString(connection, """
                SELECT pg_get_indexdef(index.indexrelid) || ':' || pg_get_expr(index.indpred, index.indrelid)
                FROM pg_index index
                JOIN pg_class index_name ON index_name.oid = index.indexrelid
                WHERE index_name.relname = 'idx_tb_post_content_search_trgm'
                """)).contains(
                "lower(", "COALESCE(title, ''::character varying)", "COALESCE(content, ''::text)",
                "enabled = 1", "is_deleted = false", "'APPROVED'::text",
                "'PRODUCT'::character varying", "'SERVICE'::character varying",
                "'LOCATION'::character varying", "'DAILY'::character varying");
        assertThat(queryForString(connection, """
                SELECT pg_get_indexdef(index.indexrelid) || ':' || pg_get_expr(index.indpred, index.indrelid)
                FROM pg_index index
                JOIN pg_class index_name ON index_name.oid = index.indexrelid
                WHERE index_name.relname = 'idx_tb_activity_content_search_trgm'
                """)).contains(
                "lower(", "COALESCE(title, ''::character varying)", "COALESCE(description, ''::text)",
                "COALESCE(address, ''::character varying)", "is_deleted = false", "'APPROVED'::text",
                "'RECRUITING'::character varying", "'ONGOING'::character varying");
    }

    private void assertColumnContracts(Connection connection) throws SQLException {
        assertThat(queryForInt(connection, """
                SELECT count(*)
                FROM information_schema.columns
                WHERE table_schema = 'petcare'
                  AND (
                      (table_name = 'tb_user' AND column_name = 'is_deleted' AND data_type = 'boolean'
                       AND is_nullable = 'NO' AND column_default = 'false')
                      OR (table_name = 'tb_pet' AND column_name = 'weight' AND data_type = 'numeric'
                          AND is_nullable = 'YES')
                      OR (table_name = 'tb_reminder' AND column_name = 'repeat_config' AND data_type = 'jsonb'
                          AND is_nullable = 'YES')
                      OR (table_name = 'tb_reminder_execution' AND column_name = 'status'
                          AND data_type = 'character varying' AND is_nullable = 'NO'
                          AND column_default LIKE '%PENDING%')
                  )
                """)).isEqualTo(4);
    }

    private void assertNoForeignKeys(Connection connection) throws SQLException {
        assertThat(queryForInt(connection, """
                SELECT count(*)
                FROM information_schema.table_constraints
                WHERE table_schema = 'petcare'
                  AND table_name LIKE 'tb_%'
                  AND constraint_type = 'FOREIGN KEY'
                """)).isZero();
    }

    private void assertReferenceData(Connection connection) throws SQLException {
        assertThat(queryForInt(connection, """
                SELECT count(*)
                FROM petcare.tb_role
                WHERE role_code IN ('admin', 'user')
                """)).isEqualTo(2);
        assertThat(queryForInt(connection, """
                SELECT count(*)
                FROM petcare.tb_points_coupon_template
                WHERE name = '新人注册券' AND source_type = 'NEWCOMER'
                """)).isEqualTo(1);
    }

    private void assertConstraintBehavior(Connection connection) throws SQLException {
        execute(connection, """
                INSERT INTO petcare.tb_reminder (pet_id, user_id, source_type, record_time, total_occurrences, completed_count)
                VALUES (1, 1, 'HEALTH_RECORD', CURRENT_TIMESTAMP, 0, 1)
                """);
        assertSqlRejected(connection, """
                INSERT INTO petcare.tb_reminder (pet_id, user_id, source_type, record_time, total_occurrences, completed_count)
                VALUES (1, 1, 'MANUAL', CURRENT_TIMESTAMP, 1, 2)
                """);
        assertSqlRejected(connection, """
                INSERT INTO petcare.tb_reminder_execution
                    (reminder_id, pet_id, user_id, schedule_time, notification_time, status)
                VALUES (1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'INVALID')
                """);
        assertSqlRejected(connection, """
                INSERT INTO petcare.tb_interaction (user_id, post_id, interaction_type, rating_value)
                VALUES (1, 1, 'RATING', 6)
                """);
        assertSqlRejected(connection, """
                INSERT INTO petcare.tb_points_account (user_id, available_points, total_points)
                VALUES (1, -1, 0)
                """);

        execute(connection, "INSERT INTO petcare.tb_user (username) VALUES ('CaseUser')");
        assertSqlRejected(connection, "INSERT INTO petcare.tb_user (username) VALUES ('caseuser')");
        assertSqlRejected(connection, "INSERT INTO petcare.tb_role (role_code, role_name) VALUES ('Admin', 'Duplicate admin')");
        execute(connection, "INSERT INTO petcare.tb_label (name) VALUES ('PetCare')");
        assertSqlRejected(connection, "INSERT INTO petcare.tb_label (name) VALUES ('petcare')");
    }

    private void assertUpdatedAtTrigger(Connection connection) throws Exception {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            long id;
            Timestamp before;
            try (var insert = connection.prepareStatement("""
                    INSERT INTO petcare.tb_role (role_code, role_name, status)
                    VALUES ('trigger_smoke', 'Trigger Smoke', 1)
                    RETURNING id, updated_at
                    """)) {
                try (ResultSet result = insert.executeQuery()) {
                    result.next();
                    id = result.getLong("id");
                    before = result.getTimestamp("updated_at");
                }
            }
            assertThat(queryForInt(connection, """
                    SELECT count(*)
                    FROM pg_trigger trigger
                    JOIN pg_class table_name ON table_name.oid = trigger.tgrelid
                    JOIN pg_namespace namespace ON namespace.oid = table_name.relnamespace
                    WHERE namespace.nspname = 'petcare'
                      AND trigger.tgname LIKE 'trg_tb_%_touch_updated_at'
                      AND NOT trigger.tgisinternal
                    """)).isEqualTo(13);
            Thread.sleep(10);
            try (var update = connection.prepareStatement("UPDATE petcare.tb_role SET status = 0 WHERE id = ?")) {
                update.setLong(1, id);
                update.executeUpdate();
            }
            try (var select = connection.prepareStatement("SELECT updated_at FROM petcare.tb_role WHERE id = ?")) {
                select.setLong(1, id);
                try (ResultSet result = select.executeQuery()) {
                    result.next();
                    assertThat(result.getTimestamp("updated_at")).isAfter(before);
                }
            }
            connection.commit();
        } finally {
            if (!connection.getAutoCommit()) {
                connection.rollback();
                connection.setAutoCommit(autoCommit);
            }
        }
    }

    private void assertFlywayHistory(Connection connection) throws SQLException {
        assertThat(queryForStrings(connection, """
                SELECT version || ':' || success
                FROM petcare.flyway_schema_history
                WHERE version IN ('1', '2', '3', '4', '5', '6', '7', '8')
                """)).containsExactlyInAnyOrder(
                "1:true", "2:true", "3:true", "4:true", "5:true", "6:true", "7:true", "8:true");
    }

    private Set<String> queryForStrings(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            var values = new java.util.HashSet<String>();
            while (result.next()) {
                values.add(result.getString(1));
            }
            return values;
        }
    }

    private int queryForInt(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }

    private String queryForString(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }

    private int executeUpdate(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            return statement.executeUpdate(sql);
        }
    }

    private void assertSqlRejected(Connection connection, String sql) {
        assertThatThrownBy(() -> execute(connection, sql)).isInstanceOf(SQLException.class);
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static Connection openConnection(String username, String password) throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), username, password);
    }
}
