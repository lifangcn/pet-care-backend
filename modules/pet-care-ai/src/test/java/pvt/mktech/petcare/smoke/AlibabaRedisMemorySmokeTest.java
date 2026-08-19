package pvt.mktech.petcare.smoke;

import com.alibaba.cloud.ai.memory.redis.LettuceRedisChatMemoryRepository;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AlibabaRedisMemorySmokeTest {

    private static final int REDIS_PORT = 6379;
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(REDIS_PORT);

    @Test
    void lettuceRepositoryAndMessageWindowMemoryCanWriteReadAndClear() {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker is required for the Redis memory smoke test");
        REDIS.start();
        try (LettuceRedisChatMemoryRepository repository = LettuceRedisChatMemoryRepository.builder()
                .host(REDIS.getHost())
                .port(REDIS.getMappedPort(REDIS_PORT))
                .timeout(3_000)
                .build()) {
            ChatMemory chatMemory = MessageWindowChatMemory.builder()
                    .chatMemoryRepository(repository)
                    .maxMessages(10)
                    .build();
            String conversationId = "redis-memory-smoke-" + UUID.randomUUID();

            chatMemory.add(conversationId, new UserMessage("redis memory smoke"));

            assertThat(chatMemory.get(conversationId)).hasSize(1);
            assertThat(repository.findByConversationId(conversationId)).hasSize(1);

            chatMemory.clear(conversationId);

            assertThat(chatMemory.get(conversationId)).isEmpty();
            assertThat(repository.findByConversationId(conversationId)).isEmpty();
        } finally {
            REDIS.stop();
        }
    }
}
