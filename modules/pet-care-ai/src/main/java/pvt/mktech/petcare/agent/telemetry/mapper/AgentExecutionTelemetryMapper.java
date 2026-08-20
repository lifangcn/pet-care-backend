package pvt.mktech.petcare.agent.telemetry.mapper;

import com.mybatisflex.annotation.UseDataSource;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import pvt.mktech.petcare.agent.telemetry.dto.AgentExecutionTelemetryRow;

/** PostgreSQL writer for agent execution telemetry. */
@Mapper
@UseDataSource("ai")
public interface AgentExecutionTelemetryMapper {

    @Insert("""
            INSERT INTO petcare.agent_execution (
                execution_id, agent_type, conversation_id, user_id, query, steps,
                final_answer, success, reason, total_steps, tool_calls, total_duration_ms,
                created_at, expires_at
            ) VALUES (
                CAST(#{executionId} AS uuid), #{agentType}, #{conversationId}, #{userId}, #{query},
                CAST(#{stepsJson} AS jsonb), #{finalAnswer}, #{success}, #{reason}, #{totalSteps},
                #{toolCalls}, #{totalDurationMs}, #{createdAt}, #{expiresAt}
            ) ON CONFLICT (execution_id) DO UPDATE SET
                agent_type = EXCLUDED.agent_type,
                conversation_id = EXCLUDED.conversation_id,
                user_id = EXCLUDED.user_id,
                query = EXCLUDED.query,
                steps = EXCLUDED.steps,
                final_answer = EXCLUDED.final_answer,
                success = EXCLUDED.success,
                reason = EXCLUDED.reason,
                total_steps = EXCLUDED.total_steps,
                tool_calls = EXCLUDED.tool_calls,
                total_duration_ms = EXCLUDED.total_duration_ms,
                created_at = EXCLUDED.created_at,
                expires_at = EXCLUDED.expires_at
            """)
    void upsert(AgentExecutionTelemetryRow row);

    @Delete("""
            WITH expired AS (
                SELECT ctid FROM petcare.agent_execution
                WHERE expires_at < CURRENT_TIMESTAMP
                ORDER BY expires_at
                FOR UPDATE SKIP LOCKED
                LIMIT #{batchSize}
            )
            DELETE FROM petcare.agent_execution
            WHERE ctid IN (SELECT ctid FROM expired)
            """)
    @UseDataSource("ai")
    int deleteExpiredBatch(@Param("batchSize") int batchSize);
}
