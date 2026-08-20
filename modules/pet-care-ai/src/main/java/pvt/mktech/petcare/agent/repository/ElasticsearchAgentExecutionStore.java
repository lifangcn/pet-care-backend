package pvt.mktech.petcare.agent.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.agent.context.AgentExecutionRecord;
import pvt.mktech.petcare.agent.context.AgentStep;
import pvt.mktech.petcare.agent.telemetry.dto.AgentExecutionDocument;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/** Elasticsearch-backed agent telemetry store. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "petcare.telemetry.store", havingValue = "elasticsearch", matchIfMissing = true)
public class ElasticsearchAgentExecutionStore implements AgentExecutionStore {

    private final ElasticsearchClient elasticsearchClient;

    @Override
    public void save(AgentExecutionRecord record) {
        AgentExecutionDocument document = toDocument(record);
        try {
            elasticsearchClient.index(IndexRequest.of(request -> request
                    .index(AgentExecutionRepository.AGENT_EXECUTION_INDEX)
                    .id(record.getExecutionId())
                    .document(document)));
        } catch (IOException exception) {
            throw new UncheckedIOException("保存 Agent 执行记录到 Elasticsearch 失败", exception);
        }
    }

    private AgentExecutionDocument toDocument(AgentExecutionRecord record) {
        List<AgentExecutionDocument.Step> steps = record.getSteps() == null ? List.of() : record.getSteps().stream()
                .map(this::toStep)
                .toList();
        return AgentExecutionDocument.builder()
                .executionId(record.getExecutionId())
                .agentType(record.getAgentType())
                .conversationId(record.getConversationId())
                .userId(record.getUserId())
                .query(record.getQuery())
                .steps(steps)
                .result(AgentExecutionDocument.Result.builder().finalAnswer(record.getFinalAnswer())
                        .success(record.isSuccess()).reason(record.getReason()).build())
                .metrics(AgentExecutionDocument.Metrics.builder().totalSteps(record.getTotalSteps())
                        .totalDurationMs(record.getTotalDurationMs()).toolCalls(record.getToolCalls()).build())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private AgentExecutionDocument.Step toStep(AgentStep step) {
        return AgentExecutionDocument.Step.builder().stepNumber(step.getStepNumber()).thought(step.getThought())
                .action(step.getAction()).toolName(step.getToolName()).toolInput(step.getToolInput())
                .observation(step.getObservation()).durationMs(step.getDurationMs()).build();
    }
}
