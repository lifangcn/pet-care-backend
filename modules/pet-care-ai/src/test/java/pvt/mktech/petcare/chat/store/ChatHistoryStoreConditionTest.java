package pvt.mktech.petcare.chat.store;

import org.junit.jupiter.api.Test;
import com.mybatisflex.annotation.UseDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatHistoryStoreConditionTest {
    @Test
    void adaptersAreMutuallySelectedByConfiguredStore() {
        ConditionalOnProperty elasticsearch = ElasticsearchChatHistoryStore.class.getAnnotation(ConditionalOnProperty.class);
        ConditionalOnProperty postgresql = PostgresqlChatHistoryStore.class.getAnnotation(ConditionalOnProperty.class);
        assertEquals("spring.ai.chat.memory.history", elasticsearch.prefix());
        assertEquals("elasticsearch", elasticsearch.havingValue());
        assertTrue(elasticsearch.matchIfMissing());
        assertEquals("postgresql", postgresql.havingValue());
        assertEquals("ai", PostgresqlChatHistoryStore.class.getAnnotation(UseDataSource.class).value());
    }
}
