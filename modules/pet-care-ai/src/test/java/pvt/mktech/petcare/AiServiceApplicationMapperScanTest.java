package pvt.mktech.petcare;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.assertj.core.api.Assertions.assertThat;

class AiServiceApplicationMapperScanTest {

    @Test
    void scansEveryAiPersistenceMapperPackage() {
        MapperScan mapperScan = AiServiceApplication.class.getAnnotation(MapperScan.class);

        assertThat(mapperScan).isNotNull();
        assertThat(mapperScan.value()).containsExactlyInAnyOrder(
                "pvt.mktech.petcare.knowledge.mapper",
                "pvt.mktech.petcare.sync.mapper",
                "pvt.mktech.petcare.observability.mapper",
                "pvt.mktech.petcare.chat.mapper",
                "pvt.mktech.petcare.agent.telemetry.mapper");
    }

    @Test
    void disablesUnusedOpenAiChatAndEmbeddingAutoConfigurations() {
        SpringBootApplication application = AiServiceApplication.class.getAnnotation(SpringBootApplication.class);

        assertThat(application.exclude()).contains(
                OpenAiChatAutoConfiguration.class,
                OpenAiEmbeddingAutoConfiguration.class);
    }
}
