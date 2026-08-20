package pvt.mktech.petcare.agent.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.annotations.Insert;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import pvt.mktech.petcare.agent.context.AgentExecutionRecord;
import pvt.mktech.petcare.agent.context.AgentStep;
import pvt.mktech.petcare.agent.telemetry.dto.AgentExecutionDocument;
import pvt.mktech.petcare.agent.telemetry.dto.AgentExecutionTelemetryRow;
import pvt.mktech.petcare.agent.telemetry.mapper.AgentExecutionTelemetryMapper;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentExecutionStoresTest {

    @Test
    void elasticsearchUsesExecutionIdAndNestedContract() throws Exception {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        when(client.index(any(IndexRequest.class))).thenReturn(null);
        AgentExecutionRecord record = sampleRecord();

        new ElasticsearchAgentExecutionStore(client).save(record);

        ArgumentCaptor<IndexRequest> captor = ArgumentCaptor.forClass(IndexRequest.class);
        verify(client).index(captor.capture());
        assertEquals("execution-id", captor.getValue().id());
        AgentExecutionDocument document = (AgentExecutionDocument) captor.getValue().document();
        assertEquals("final answer", document.getResult().getFinalAnswer());
        assertEquals(1, document.getMetrics().getTotalSteps());
        assertEquals("search", document.getSteps().getFirst().getToolName());
    }

    @Test
    void postgresqlSerializesOnlyStepsAndUsesTypedRetentionRow() {
        AgentExecutionTelemetryMapper mapper = mock(AgentExecutionTelemetryMapper.class);
        PostgresqlAgentExecutionStore store = new PostgresqlAgentExecutionStore(mapper, new ObjectMapper());
        ReflectionTestUtils.setField(store, "retentionDays", 7L);
        AgentExecutionRecord record = sampleRecord();

        store.save(record);

        ArgumentCaptor<AgentExecutionTelemetryRow> captor = ArgumentCaptor.forClass(AgentExecutionTelemetryRow.class);
        verify(mapper).upsert(captor.capture());
        AgentExecutionTelemetryRow row = captor.getValue();
        assertEquals("execution-id", row.getExecutionId());
        assertEquals("[{\"stepNumber\":1,\"thought\":\"thought\",\"action\":\"TOOL_CALL\",\"toolName\":\"search\",\"toolInput\":\"dogs\",\"observation\":\"found\",\"durationMs\":12}]", row.getStepsJson());
        assertEquals(42L, row.getTotalDurationMs());
        assertEquals(record.getCreatedAt().plusSeconds(7 * 24 * 60 * 60), row.getExpiresAt());
    }

    @Test
    void postgresqlMapperCastsUuidAndJsonbAndUpsertsAllMutableColumns() throws Exception {
        String sql = AgentExecutionTelemetryMapper.class.getMethod("upsert", AgentExecutionTelemetryRow.class)
                .getAnnotation(Insert.class).value()[0];

        assertTrue(sql.contains("CAST(#{executionId} AS uuid)"));
        assertTrue(sql.contains("CAST(#{stepsJson} AS jsonb)"));
        assertTrue(sql.contains("ON CONFLICT (execution_id) DO UPDATE SET"));
        assertTrue(sql.contains("expires_at = EXCLUDED.expires_at"));
    }

    @Test
    void postgresqlNormalizesNullStepsAndRejectsMissingRequiredValues() {
        AgentExecutionTelemetryMapper mapper = mock(AgentExecutionTelemetryMapper.class);
        PostgresqlAgentExecutionStore store = new PostgresqlAgentExecutionStore(mapper, new ObjectMapper());
        ReflectionTestUtils.setField(store, "retentionDays", 7L);
        AgentExecutionRecord record = sampleRecord();
        record.setSteps(null);

        store.save(record);

        ArgumentCaptor<AgentExecutionTelemetryRow> captor = ArgumentCaptor.forClass(AgentExecutionTelemetryRow.class);
        verify(mapper).upsert(captor.capture());
        assertEquals("[]", captor.getValue().getStepsJson());
        record.setCreatedAt(null);
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> store.save(record));
    }

    private AgentExecutionRecord sampleRecord() {
        return AgentExecutionRecord.builder()
                .executionId("execution-id").agentType("REACT").conversationId("conversation")
                .userId(10L).query("query")
                .steps(List.of(AgentStep.builder().stepNumber(1).thought("thought").action("TOOL_CALL")
                        .toolName("search").toolInput("dogs").observation("found").durationMs(12).build()))
                .finalAnswer("final answer").success(true).totalSteps(1).toolCalls(1).totalDurationMs(42)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z")).build();
    }
}
