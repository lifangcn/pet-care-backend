package pvt.mktech.petcare.agent.repository;

import pvt.mktech.petcare.agent.context.AgentExecutionRecord;

/** Narrow persistence port for agent execution telemetry. */
public interface AgentExecutionStore {

    void save(AgentExecutionRecord record);
}
