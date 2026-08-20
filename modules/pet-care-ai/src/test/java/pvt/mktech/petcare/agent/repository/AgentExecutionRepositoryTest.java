package pvt.mktech.petcare.agent.repository;

import org.junit.jupiter.api.Test;
import pvt.mktech.petcare.agent.context.AgentExecutionRecord;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AgentExecutionRepositoryTest {

    @Test
    void delegatesToStore() {
        AgentExecutionStore store = mock(AgentExecutionStore.class);
        AgentExecutionRecord record = AgentExecutionRecord.builder().executionId("execution-id").build();

        new AgentExecutionRepository(store).save(record);

        verify(store).save(record);
    }

    @Test
    void propagatesStoreException() {
        AgentExecutionStore store = mock(AgentExecutionStore.class);
        AgentExecutionRecord record = AgentExecutionRecord.builder().build();
        RuntimeException expected = new IllegalStateException("store unavailable");
        doThrow(expected).when(store).save(record);

        assertSame(expected, assertThrows(RuntimeException.class,
                () -> new AgentExecutionRepository(store).save(record)));
    }
}
