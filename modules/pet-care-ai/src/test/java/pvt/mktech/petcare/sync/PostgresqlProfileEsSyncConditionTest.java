package pvt.mktech.petcare.sync;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.ElasticsearchTransport;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import pvt.mktech.petcare.chat.service.HybridSearchService;
import pvt.mktech.petcare.infrastructure.config.ElasticsearchConfig;
import pvt.mktech.petcare.infrastructure.config.IndexInitializationRunner;
import pvt.mktech.petcare.sync.consumer.ActivityCdcListener;
import pvt.mktech.petcare.sync.consumer.PostCdcListener;
import pvt.mktech.petcare.sync.controller.DataSyncController;
import pvt.mktech.petcare.sync.controller.admin.AdminDataSyncController;
import pvt.mktech.petcare.sync.schedule.HighFreqFieldSyncScheduler;
import pvt.mktech.petcare.sync.service.CdcHandlerService;
import pvt.mktech.petcare.sync.service.DataMigrationService;
import pvt.mktech.petcare.sync.service.IndexAdminService;
import pvt.mktech.petcare.sync.service.impl.SyncServiceImpl;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresqlProfileEsSyncConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    ElasticsearchConfig.class,
                    HybridSearchService.class,
                    SyncServiceImpl.class,
                    CdcHandlerService.class,
                    DataMigrationService.class,
                    PostCdcListener.class,
                    ActivityCdcListener.class,
                    HighFreqFieldSyncScheduler.class,
                    IndexAdminService.class,
                    DataSyncController.class,
                    AdminDataSyncController.class,
                    IndexInitializationRunner.class)
            .withPropertyValues(
                    "petcare.elasticsearch.enabled=false",
                    "petcare.search.content-backend=postgresql",
                    "petcare.sync.enabled=false",
                    "es.index.auto-init=false");

    @Test
    void disablesElasticsearchAndSyncBeansWithoutCreatingExternalClients() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ElasticsearchClient.class);
            assertThat(context).doesNotHaveBean(RestClient.class);
            assertThat(context).doesNotHaveBean(HybridSearchService.class);
            assertThat(context).doesNotHaveBean(SyncServiceImpl.class);
            assertThat(context).doesNotHaveBean(CdcHandlerService.class);
            assertThat(context).doesNotHaveBean(DataMigrationService.class);
            assertThat(context).doesNotHaveBean(PostCdcListener.class);
            assertThat(context).doesNotHaveBean(ActivityCdcListener.class);
            assertThat(context).doesNotHaveBean(HighFreqFieldSyncScheduler.class);
            assertThat(context).doesNotHaveBean(IndexAdminService.class);
            assertThat(context).doesNotHaveBean(DataSyncController.class);
            assertThat(context).doesNotHaveBean(AdminDataSyncController.class);
            assertThat(context).doesNotHaveBean(IndexInitializationRunner.class);
        });
    }

    @Test
    void postgresqlProfileDisablesEsAndKafkaInfrastructure() {
        YamlPropertiesFactoryBean loader = new YamlPropertiesFactoryBean();
        loader.setResources(new ClassPathResource("application-postgresql.yml"));
        Properties properties = loader.getObject();

        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("petcare.elasticsearch.enabled")).isEqualTo("false");
        assertThat(properties.getProperty("petcare.sync.enabled")).isEqualTo("false");
        assertThat(properties.getProperty("es.index.auto-init")).isEqualTo("false");
        assertThat(properties.getProperty("spring.kafka.bootstrap-servers")).isEmpty();
        assertThat(properties.getProperty("spring.autoconfigure.exclude[0]"))
                .isEqualTo("org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration");
        assertThat(properties.getProperty("spring.autoconfigure.exclude[1]"))
                .isEqualTo("org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration");
    }

    @Test
    void postgresqlProfileConfigDataExcludesElasticsearchAndKafkaAutoConfigurationsWithoutExternalClients() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=postgresql")
                .withUserConfiguration(PostgresqlProfileAutoConfigurationTestApplication.class)
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNull();
                    assertThat(context.getEnvironment().getActiveProfiles()).contains("postgresql");
                    assertThat(context.getEnvironment().getProperty("petcare.elasticsearch.enabled")).isEqualTo("false");
                    assertThat(context.getEnvironment().getProperty("petcare.sync.enabled")).isEqualTo("false");
                    assertThat(context.getEnvironment().getProperty("es.index.auto-init")).isEqualTo("false");

                    assertThat(context).doesNotHaveBean(RestClient.class);
                    assertThat(context).doesNotHaveBean(ElasticsearchTransport.class);
                    assertThat(context).doesNotHaveBean(ElasticsearchClient.class);
                    assertThat(context).doesNotHaveBean(KafkaTemplate.class);
                    assertThat(context).doesNotHaveBean(ProducerFactory.class);
                    assertThat(context).doesNotHaveBean(ConsumerFactory.class);
                    assertThat(context).doesNotHaveBean(KafkaAdmin.class);
                    assertThat(context).doesNotHaveBean(KafkaListenerEndpointRegistry.class);
                });
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration(excludeName = {
            "com.github.xiaoymin.knife4j.spring.configuration.Knife4jAutoConfiguration",
            "com.mybatisflex.spring.boot.MybatisFlexAutoConfiguration",
            "org.redisson.spring.starter.RedissonAutoConfigurationV2",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration",
            "org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiChatAutoConfiguration",
            "org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiEmbeddingAutoConfiguration",
            "org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiImageAutoConfiguration",
            "org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration",
            "pvt.mktech.petcare.common.redis.RedisAutoConfiguration",
            "pvt.mktech.petcare.common.storage.FileStorageAutoConfiguration"
    })
    static class PostgresqlProfileAutoConfigurationTestApplication {
    }
}
