package pvt.mktech.petcare.agent.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/** Elasticsearch contract for an agent execution. */
@Data
@Builder
public class AgentExecutionDocument {
    @JsonProperty("execution_id")
    private String executionId;
    @JsonProperty("agent_type")
    private String agentType;
    @JsonProperty("conversation_id")
    private String conversationId;
    @JsonProperty("user_id")
    private Long userId;
    private String query;
    private List<Step> steps;
    private Result result;
    private Metrics metrics;
    @JsonProperty("created_at")
    private Instant createdAt;

    @Data
    @Builder
    public static class Step {
        @JsonProperty("step_number")
        private int stepNumber;
        private String thought;
        private String action;
        @JsonProperty("tool_name")
        private String toolName;
        @JsonProperty("tool_input")
        private String toolInput;
        private String observation;
        @JsonProperty("duration_ms")
        private long durationMs;
    }

    @Data
    @Builder
    public static class Result {
        @JsonProperty("final_answer")
        private String finalAnswer;
        private boolean success;
        private String reason;
    }

    @Data
    @Builder
    public static class Metrics {
        @JsonProperty("total_steps")
        private int totalSteps;
        @JsonProperty("total_duration_ms")
        private long totalDurationMs;
        @JsonProperty("tool_calls")
        private int toolCalls;
    }
}
