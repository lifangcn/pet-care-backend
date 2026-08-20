package pvt.mktech.petcare.agent.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.agent.context.AgentExecutionRecord;
import pvt.mktech.petcare.agent.telemetry.dto.AgentExecutionTelemetryRow;
import pvt.mktech.petcare.agent.telemetry.mapper.AgentExecutionTelemetryMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/** PostgreSQL-backed agent telemetry store. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "petcare.telemetry.store", havingValue = "postgresql")
public class PostgresqlAgentExecutionStore implements AgentExecutionStore {

    private final AgentExecutionTelemetryMapper telemetryMapper;
    private final ObjectMapper objectMapper;
    @Value("${petcare.telemetry.retention-days:30}")
    private long retentionDays;

    @Override
    public void save(AgentExecutionRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        Objects.requireNonNull(record.getExecutionId(), "executionId must not be null");
        Objects.requireNonNull(record.getAgentType(), "agentType must not be null");
        Objects.requireNonNull(record.getQuery(), "query must not be null");
        Instant createdAt = record.getCreatedAt();
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (retentionDays <= 0) {
            throw new IllegalArgumentException("petcare.telemetry.retention-days must be greater than zero");
        }
        telemetryMapper.upsert(AgentExecutionTelemetryRow.builder()
                .executionId(record.getExecutionId())
                .agentType(record.getAgentType())
                .conversationId(record.getConversationId())
                .userId(record.getUserId())
                .query(record.getQuery())
                .stepsJson(serializeSteps(record))
                .finalAnswer(record.getFinalAnswer())
                .success(record.isSuccess())
                .reason(record.getReason())
                .totalSteps(record.getTotalSteps())
                .toolCalls(record.getToolCalls())
                .totalDurationMs(record.getTotalDurationMs())
                .createdAt(createdAt)
                .expiresAt(createdAt.plus(retentionDays, ChronoUnit.DAYS))
                .build());
    }

    private String serializeSteps(AgentExecutionRecord record) {
        try {
            List<?> steps = record.getSteps();
            return objectMapper.writeValueAsString(steps == null ? List.of() : steps);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("序列化 Agent 执行步骤失败", exception);
        }
    }
}
