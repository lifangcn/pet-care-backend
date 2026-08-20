package pvt.mktech.petcare.agent.telemetry.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/** Typed PostgreSQL row for agent execution telemetry. */
@Value
@Builder
public class AgentExecutionTelemetryRow {
    String executionId;
    String agentType;
    String conversationId;
    Long userId;
    String query;
    String stepsJson;
    String finalAnswer;
    boolean success;
    String reason;
    int totalSteps;
    int toolCalls;
    long totalDurationMs;
    Instant createdAt;
    Instant expiresAt;
}
