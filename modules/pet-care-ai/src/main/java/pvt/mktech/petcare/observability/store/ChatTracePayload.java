package pvt.mktech.petcare.observability.store;

import java.time.Instant;

/** PostgreSQL-ready representation of a chat trace. */
public record ChatTracePayload(
        String traceId,
        String conversationId,
        String sessionId,
        Long userId,
        Instant occurredAt,
        Integer durationMs,
        String requestJson,
        String responseJson,
        String ragJson,
        String toolCallsJson,
        String errorJson,
        String metadataJson) {
}
