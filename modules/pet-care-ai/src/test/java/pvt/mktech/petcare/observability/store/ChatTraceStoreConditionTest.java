package pvt.mktech.petcare.observability.store;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatTraceStoreConditionTest {

    @Test
    void selectsStoresUsingTelemetryStoreProperty() {
        ConditionalOnProperty elasticsearch = ElasticsearchChatTraceStore.class.getAnnotation(ConditionalOnProperty.class);
        ConditionalOnProperty postgresql = PostgresqlChatTraceStore.class.getAnnotation(ConditionalOnProperty.class);

        assertEquals("petcare.telemetry", elasticsearch.prefix());
        assertEquals("elasticsearch", elasticsearch.havingValue());
        assertTrue(elasticsearch.matchIfMissing());
        assertEquals("postgresql", postgresql.havingValue());
    }
}
