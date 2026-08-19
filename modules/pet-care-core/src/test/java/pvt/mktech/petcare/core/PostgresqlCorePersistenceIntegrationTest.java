package pvt.mktech.petcare.core;

import com.mybatisflex.core.mybatis.FlexConfiguration;
import com.mybatisflex.core.query.QueryMethods;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.datasource.FlexDataSource;
import com.mybatisflex.core.dialect.DbType;
import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.table.TableInfoFactory;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import pvt.mktech.petcare.reminder.entity.Reminder;
import pvt.mktech.petcare.reminder.entity.codelist.RepeatTypeOfReminder;
import pvt.mktech.petcare.reminder.entity.codelist.SourceTypeOfReminder;
import pvt.mktech.petcare.reminder.mapper.ReminderMapper;
import pvt.mktech.petcare.social.entity.Activity;
import pvt.mktech.petcare.social.entity.Post;
import pvt.mktech.petcare.social.entity.codelist.TypeOfActivity;
import pvt.mktech.petcare.social.entity.codelist.TypeOfPost;
import pvt.mktech.petcare.social.mapper.ActivityMapper;
import pvt.mktech.petcare.social.mapper.PostMapper;
import pvt.mktech.petcare.social.handler.StringListTypeHandler;
import pvt.mktech.petcare.user.entity.User;
import pvt.mktech.petcare.user.mapper.UserMapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static pvt.mktech.petcare.user.entity.table.UserTableDef.USER;
import static pvt.mktech.petcare.reminder.entity.table.ReminderTableDef.REMINDER;
import static pvt.mktech.petcare.social.entity.table.PostTableDef.POST;
import static pvt.mktech.petcare.social.entity.table.ActivityTableDef.ACTIVITY;

class PostgresqlCorePersistenceIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16"));
    private static final String MIGRATION_OWNER = "core_migration_owner";
    private static final String APP_LOGIN = "core_application_login";
    private static final String MIGRATION_PASSWORD = UUID.randomUUID().toString();
    private static final String APP_PASSWORD = UUID.randomUUID().toString();
    private static SqlSessionFactory sqlSessionFactory;

    @BeforeAll
    static void setUp() throws Exception {
        POSTGRES.start();
        try (Connection connection = connect(POSTGRES.getUsername(), POSTGRES.getPassword()); var statement = connection.createStatement()) {
            statement.execute("CREATE ROLE petcare_app NOLOGIN");
            // The migration owner is temporarily superuser because pgvector extension installation requires it.
            statement.execute("CREATE ROLE " + MIGRATION_OWNER + " LOGIN SUPERUSER PASSWORD '" + MIGRATION_PASSWORD + "'");
            statement.execute("CREATE ROLE " + APP_LOGIN + " LOGIN NOSUPERUSER PASSWORD '" + APP_PASSWORD + "'");
            statement.execute("GRANT petcare_app TO " + APP_LOGIN);
        }
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), MIGRATION_OWNER, MIGRATION_PASSWORD)
                .locations("classpath:db/migration").schemas("petcare").defaultSchema("petcare").createSchemas(true).load().migrate();

        FlexGlobalConfig.getDefaultConfig().setDbType(DbType.POSTGRE_SQL);
        PooledDataSource dataSource = new PooledDataSource("org.postgresql.Driver", POSTGRES.getJdbcUrl() + "&currentSchema=petcare", APP_LOGIN, APP_PASSWORD);
        FlexConfiguration configuration = new FlexConfiguration();
        configuration.setUseGeneratedKeys(true);
        configuration.setEnvironment(new Environment("postgresql", new JdbcTransactionFactory(), new FlexDataSource("default", dataSource, DbType.POSTGRE_SQL, true)));
        FlexGlobalConfig.getDefaultConfig().setConfiguration(configuration);
        configuration.addMapper(UserMapper.class);
        configuration.addMapper(ReminderMapper.class);
        configuration.addMapper(PostMapper.class);
        configuration.addMapper(ActivityMapper.class);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        FlexGlobalConfig.getDefaultConfig().setSqlSessionFactory(sqlSessionFactory);
        assertColumnTypeHandler(Reminder.class, "repeatConfig", "JsonStringTypeHandler");
        assertColumnTypeHandler(Post.class, "mediaUrls", StringListTypeHandler.class.getSimpleName());
        assertColumnTypeHandler(Activity.class, "labels", StringListTypeHandler.class.getSimpleName());
    }

    @AfterAll
    static void tearDown() {
        POSTGRES.stop();
    }

    @Test
    void persistsCoreEntitiesThroughProjectMappersAndRollsBack() throws Exception {
        String username = "CaseUser" + UUID.randomUUID();
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            UserMapper userMapper = session.getMapper(UserMapper.class);
            ReminderMapper reminderMapper = session.getMapper(ReminderMapper.class);
            PostMapper postMapper = session.getMapper(PostMapper.class);
            ActivityMapper activityMapper = session.getMapper(ActivityMapper.class);

            User user = new User();
            user.setUsername(username);
            assertThat(userMapper.insertSelective(user)).isEqualTo(1);
            user = userMapper.selectOneByQuery(QueryWrapper.create().where(USER.USERNAME.eq(username)));
            assertThat(user.getId()).isNotNull();
            User storedUser = user;
            assertThat(storedUser.getIsDeleted()).isFalse();
            assertThat(userMapper.selectCountByQuery(QueryWrapper.create()
                    .where(QueryMethods.lower(USER.USERNAME).eq(username.toLowerCase(java.util.Locale.ROOT))))).isEqualTo(1);

            Reminder reminder = new Reminder();
            reminder.setPetId(1L);
            reminder.setUserId(user.getId());
            reminder.setSourceType(SourceTypeOfReminder.HEALTH_RECORD);
            reminder.setRepeatType(RepeatTypeOfReminder.CUSTOM);
            reminder.setRepeatConfig("{\"周期\":\"每周\",\"days\":[1,3]}");
            reminder.setRecordTime(LocalDateTime.now());
            assertThat(reminderMapper.insertSelective(reminder)).isEqualTo(1);
            reminder = reminderMapper.selectOneByQuery(QueryWrapper.create().where(REMINDER.USER_ID.eq(user.getId())));
            assertThat(reminderMapper.selectOneById(reminder.getId()).getRepeatConfig()).isEqualTo(reminder.getRepeatConfig());
            assertThat(reminderMapper.selectOneById(reminder.getId()).getSourceType()).isEqualTo(SourceTypeOfReminder.HEALTH_RECORD);

            Post post = new Post();
            post.setUserId(user.getId());
            post.setPostType(TypeOfPost.DAILY);
            post.setMediaUrls(List.of("中文\"引号\"", "https://example.test/a"));
            assertThat(postMapper.insertSelective(post)).isEqualTo(1);
            post = postMapper.selectOneByQuery(QueryWrapper.create().where(POST.USER_ID.eq(user.getId())));
            assertThat(postMapper.selectOneById(post.getId()).getMediaUrls()).containsExactlyElementsOf(post.getMediaUrls());

            Activity activity = new Activity();
            activity.setUserId(user.getId());
            activity.setTitle("空标签活动");
            activity.setActivityType(TypeOfActivity.ONLINE);
            activity.setActivityTime(LocalDateTime.now());
            activity.setLabels(List.of());
            assertThat(activityMapper.insertSelective(activity)).isEqualTo(1);
            activity = activityMapper.selectOneByQuery(QueryWrapper.create().where(ACTIVITY.USER_ID.eq(user.getId())));
            assertThat(activityMapper.selectOneById(activity.getId()).getLabels()).isEmpty();

            LocalDateTime updatedAt = storedUser.getUpdatedAt();
            Thread.sleep(10);
            storedUser.setNickname("updated");
            userMapper.update(storedUser);
            assertThat(userMapper.selectOneById(user.getId()).getUpdatedAt()).isAfter(updatedAt);
            session.rollback();
        }
        try (SqlSession session = sqlSessionFactory.openSession()) {
            assertThat(session.getMapper(UserMapper.class).selectCountByQuery(QueryWrapper.create().where(USER.USERNAME.eq(username)))).isZero();
        }
    }

    private static Connection connect(String username, String password) throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), username, password);
    }

    private static void assertColumnTypeHandler(Class<?> entityClass, String property, String handlerName) {
        assertThat(TableInfoFactory.ofEntityClass(entityClass).getColumnInfoList()).anySatisfy(column -> {
            assertThat(column.getProperty()).isEqualTo(property);
            assertThat(column.buildTypeHandler(FlexGlobalConfig.getDefaultConfig().getConfiguration()).getClass().getSimpleName())
                    .isEqualTo(handlerName);
        });
    }
}
