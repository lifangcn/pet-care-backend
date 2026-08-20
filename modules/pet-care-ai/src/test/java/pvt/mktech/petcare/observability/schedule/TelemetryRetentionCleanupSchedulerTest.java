package pvt.mktech.petcare.observability.schedule;

import org.junit.jupiter.api.Test;
import pvt.mktech.petcare.agent.telemetry.mapper.AgentExecutionTelemetryMapper;
import pvt.mktech.petcare.observability.config.TelemetryProperties;
import pvt.mktech.petcare.observability.mapper.ChatTraceMapper;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelemetryRetentionCleanupSchedulerTest {

    @Test
    void limitsEachTableToConfiguredBatches() {
        ChatTraceMapper chat = mock(ChatTraceMapper.class);
        AgentExecutionTelemetryMapper agent = mock(AgentExecutionTelemetryMapper.class);
        when(chat.deleteExpiredBatch(2)).thenReturn(2);
        when(agent.deleteExpiredBatch(2)).thenReturn(2);

        scheduler(chat, agent).cleanupExpiredTelemetry();

        verify(chat, times(3)).deleteExpiredBatch(2);
        verify(agent, times(3)).deleteExpiredBatch(2);
    }

    @Test
    void continuesWithAgentCleanupWhenChatCleanupFails() {
        ChatTraceMapper chat = mock(ChatTraceMapper.class);
        AgentExecutionTelemetryMapper agent = mock(AgentExecutionTelemetryMapper.class);
        doThrow(new IllegalStateException("database unavailable")).when(chat).deleteExpiredBatch(anyInt());
        when(agent.deleteExpiredBatch(2)).thenReturn(0);

        scheduler(chat, agent).cleanupExpiredTelemetry();

        verify(agent).deleteExpiredBatch(2);
    }

    private TelemetryRetentionCleanupScheduler scheduler(ChatTraceMapper chat, AgentExecutionTelemetryMapper agent) {
        TelemetryProperties properties = new TelemetryProperties();
        properties.getCleanup().setBatchSize(2);
        properties.getCleanup().setMaxBatches(3);
        return new TelemetryRetentionCleanupScheduler(chat, agent, properties);
    }
}
