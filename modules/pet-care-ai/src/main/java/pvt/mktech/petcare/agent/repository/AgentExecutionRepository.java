package pvt.mktech.petcare.agent.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pvt.mktech.petcare.agent.context.AgentExecutionRecord;

/** Agent execution persistence facade used by ReactAgent. */
@Repository
@RequiredArgsConstructor
public class AgentExecutionRepository {

    public static final String AGENT_EXECUTION_INDEX = "agent_execution";

    private final AgentExecutionStore executionStore;

    public void save(AgentExecutionRecord record) {
        executionStore.save(record);
    }
}
