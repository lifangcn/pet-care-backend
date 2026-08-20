package pvt.mktech.petcare.agent.core;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import pvt.mktech.petcare.agent.config.AgentProperties;
import pvt.mktech.petcare.agent.context.AgentContext;
import pvt.mktech.petcare.agent.context.AgentExecutionRecord;
import pvt.mktech.petcare.agent.repository.AgentExecutionRepository;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReactAgentAdapterTest {

    @Test
    void initializationFailureGetsIdAndActualDuration() throws Exception {
        ReactAgent reactAgent = mock(ReactAgent.class);
        when(reactAgent.getAndCompileGraph()).thenThrow(new IllegalStateException("initialization failed"));
        AgentExecutionRepository repository = mock(AgentExecutionRepository.class);
        AgentContext context = AgentContext.builder().userId(1L).conversationId("conversation").build();
        long before = System.currentTimeMillis();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> new ReactAgentAdapter(reactAgent, new AgentProperties(), repository)
                        .executeStreaming("query", context).collectList().block());

        assertTrue(exception.getMessage().contains("initialization failed"));
        assertNotNull(context.getExecutionId());
        ArgumentCaptor<AgentExecutionRecord> captor = ArgumentCaptor.forClass(AgentExecutionRecord.class);
        verify(repository).save(captor.capture());
        assertTrue(!captor.getValue().isSuccess());
        assertTrue(captor.getValue().getTotalDurationMs() >= 0);
        assertTrue(captor.getValue().getCreatedAt().toEpochMilli() >= before);
    }

    @Test
    void persistenceFailureDoesNotReplaceOriginalExecutionFailure() throws Exception {
        ReactAgent reactAgent = mock(ReactAgent.class);
        IllegalStateException original = new IllegalStateException("graph failed");
        when(reactAgent.getAndCompileGraph()).thenThrow(original);
        AgentExecutionRepository repository = mock(AgentExecutionRepository.class);
        doThrow(new IllegalStateException("telemetry failed")).when(repository).save(org.mockito.ArgumentMatchers.any());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> new ReactAgentAdapter(reactAgent, new AgentProperties(), repository)
                        .executeStreaming("query", AgentContext.builder().userId(1L).conversationId("conversation").build()).collectList().block());

        assertTrue(exception == original || exception.getCause() == original);
    }
}
