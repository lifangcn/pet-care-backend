package pvt.mktech.petcare.observability.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import pvt.mktech.petcare.observability.config.TelemetryProperties;
import pvt.mktech.petcare.observability.dto.ChatTraceDocument;
import pvt.mktech.petcare.observability.mapper.ChatTraceMapper;

import java.util.List;
import java.util.Objects;

/** PostgreSQL-backed chat trace store. */
@ConditionalOnProperty(prefix = "petcare.telemetry", name = "store", havingValue = "postgresql")
public class PostgresqlChatTraceStore implements ChatTraceStore {

    private final ChatTraceMapper chatTraceMapper;
    private final ObjectMapper objectMapper;
    private final TelemetryProperties telemetryProperties;

    public PostgresqlChatTraceStore(ChatTraceMapper chatTraceMapper, ObjectMapper objectMapper,
                                    TelemetryProperties telemetryProperties) {
        this.chatTraceMapper = chatTraceMapper;
        this.objectMapper = objectMapper;
        this.telemetryProperties = telemetryProperties;
    }

    @Override
    public void save(ChatTraceDocument document) {
        Objects.requireNonNull(document, "document must not be null");
        Objects.requireNonNull(document.getTraceId(), "traceId must not be null");
        Objects.requireNonNull(document.getTimestamp(), "timestamp must not be null");
        requirePositiveRetentionDays();
        try {
            ChatTracePayload payload = new ChatTracePayload(
                    document.getTraceId(), document.getConversationId(), document.getSessionId(), document.getUserId(),
                    document.getTimestamp(), document.getDurationMs(), jsonOrEmptyObject(document.getRequest()), jsonOrEmptyObject(document.getResponse()),
                    json(document.getRag()), jsonOrEmptyArray(document.getToolCalls()), json(document.getError()), jsonOrEmptyObject(document.getMetadata()));
            chatTraceMapper.upsert(payload, telemetryProperties.getRetentionDays());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize chat trace for PostgreSQL", exception);
        }
    }

    private String json(Object value) throws JsonProcessingException {
        return value == null ? null : objectMapper.writeValueAsString(value);
    }

    private String jsonOrEmptyObject(Object value) throws JsonProcessingException {
        return value == null ? "{}" : json(value);
    }

    private String jsonOrEmptyArray(List<?> value) throws JsonProcessingException {
        return value == null ? "[]" : json(value);
    }

    private void requirePositiveRetentionDays() {
        if (telemetryProperties.getRetentionDays() <= 0) {
            throw new IllegalArgumentException("petcare.telemetry.retention-days must be greater than zero");
        }
    }
}
