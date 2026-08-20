package pvt.mktech.petcare.observability.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import pvt.mktech.petcare.observability.config.TelemetryProperties;
import pvt.mktech.petcare.observability.dto.ChatTraceDocument;
import pvt.mktech.petcare.observability.mapper.ChatTraceMapper;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PostgresqlChatTraceStoreTest {

    @Test
    void serializesNestedFieldsAndUpsertsWithRetention() {
        ChatTraceMapper mapper = mock(ChatTraceMapper.class);
        TelemetryProperties properties = new TelemetryProperties();
        properties.setRetentionDays(14);
        ChatTraceDocument document = new ChatTraceDocument();
        document.setTraceId("b4f1e60b-f1b5-412d-93d2-1712f7633263");
        document.setTimestamp(Instant.parse("2026-03-06T12:00:00Z"));
        ChatTraceDocument.RequestInfo request = new ChatTraceDocument.RequestInfo();
        request.setContent("hello");
        document.setRequest(request);
        document.setToolCalls(List.of());

        new PostgresqlChatTraceStore(mapper, new ObjectMapper(), properties).save(document);

        ArgumentCaptor<ChatTracePayload> payload = ArgumentCaptor.forClass(ChatTracePayload.class);
        verify(mapper).upsert(payload.capture(), org.mockito.ArgumentMatchers.eq(14));
        assertEquals(document.getTraceId(), payload.getValue().traceId());
        assertEquals("{\"content\":\"hello\",\"tokens\":null}", payload.getValue().requestJson());
        assertEquals("[]", payload.getValue().toolCallsJson());
        assertEquals("{}", payload.getValue().responseJson());
        assertEquals("{}", payload.getValue().metadataJson());
        assertEquals(null, payload.getValue().ragJson());
        assertEquals(null, payload.getValue().errorJson());
    }
}
