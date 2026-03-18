package pvt.mktech.petcare.agent.orchestrator;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.agent.config.AgentProperties;
import pvt.mktech.petcare.agent.core.Agent;
import pvt.mktech.petcare.agent.core.ReactAgentAdapter;

import java.util.List;

/**
 * {@code @description}: Agent 调度器 - 根据查询复杂度选择合适的 Agent
 * {@code @date}: 2026-03-17
 * @author Michael Li
 */
@Slf4j
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final AgentProperties agentProperties;
    private final List<Agent> agents;
    private final ReactAgent reactAgent;

    /**
     * 根据用户查询选择合适的 Agent
     * <p>
     * 目前简化实现：默认使用配置的默认类型
     * 后续可扩展意图识别分类，根据复杂程度自动选择
     */
    public Agent selectAgent(String userQuery) {
        AgentProperties.AgentType defaultType = agentProperties.getDefaultType();

        // TODO: 后续可添加意图识别，根据查询复杂度自动选择
        // 简单问题 -> SimpleAgent，多步问题 -> ReactAgent，复杂规划 -> PlanAndExecute

        for (Agent agent : agents) {
            if (agent.getType().equalsIgnoreCase(defaultType.name())) {
                return agent;
            }
        }

        // 默认返回 ReAct Agent（适配多步推理需求）
        return getDefaultAgent();
    }

    /**
     * 获取默认 Agent
     */
    private Agent getDefaultAgent() {
        return agents.stream()
                .filter(a -> a.getType().equals(AgentProperties.AgentType.REACT.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No REACT Agent configured"));
    }
}
