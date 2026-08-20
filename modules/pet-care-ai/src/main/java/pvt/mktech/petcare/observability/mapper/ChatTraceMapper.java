package pvt.mktech.petcare.observability.mapper;

import com.mybatisflex.annotation.UseDataSource;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import pvt.mktech.petcare.observability.store.ChatTracePayload;

/** PostgreSQL persistence operations for chat traces. */
@Mapper
@UseDataSource("ai")
public interface ChatTraceMapper {

    @Insert("""
            INSERT INTO petcare.chat_trace (trace_id, conversation_id, session_id, user_id, occurred_at, duration_ms,
                                            request_data, response_data, rag_data, tool_calls, error_data, metadata, expires_at)
            VALUES (CAST(#{payload.traceId} AS uuid), #{payload.conversationId}, #{payload.sessionId}, #{payload.userId},
                    #{payload.occurredAt}, #{payload.durationMs}, CAST(#{payload.requestJson} AS jsonb),
                    CAST(#{payload.responseJson} AS jsonb), CAST(#{payload.ragJson} AS jsonb),
                    CAST(#{payload.toolCallsJson} AS jsonb), CAST(#{payload.errorJson} AS jsonb),
                    CAST(#{payload.metadataJson} AS jsonb), CAST(#{payload.occurredAt} AS timestamptz) + (#{retentionDays} * INTERVAL '1 day'))
            ON CONFLICT (trace_id) DO UPDATE SET
              trace_id = EXCLUDED.trace_id,
              conversation_id = EXCLUDED.conversation_id,
              session_id = EXCLUDED.session_id,
              user_id = EXCLUDED.user_id,
              occurred_at = EXCLUDED.occurred_at,
              duration_ms = EXCLUDED.duration_ms,
              request_data = EXCLUDED.request_data,
              response_data = EXCLUDED.response_data,
              rag_data = EXCLUDED.rag_data,
              tool_calls = EXCLUDED.tool_calls,
              error_data = EXCLUDED.error_data,
              metadata = EXCLUDED.metadata,
              expires_at = EXCLUDED.expires_at
            """)
    void upsert(@Param("payload") ChatTracePayload payload, @Param("retentionDays") int retentionDays);

    @Delete("""
            WITH expired AS (
                SELECT ctid FROM petcare.chat_trace
                WHERE expires_at < CURRENT_TIMESTAMP
                ORDER BY expires_at
                FOR UPDATE SKIP LOCKED
                LIMIT #{batchSize}
            )
            DELETE FROM petcare.chat_trace
            WHERE ctid IN (SELECT ctid FROM expired)
            """)
    @UseDataSource("ai")
    int deleteExpiredBatch(@Param("batchSize") int batchSize);
}
