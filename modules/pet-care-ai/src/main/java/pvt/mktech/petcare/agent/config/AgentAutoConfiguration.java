package pvt.mktech.petcare.agent.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pvt.mktech.petcare.agent.core.Agent;
import pvt.mktech.petcare.agent.core.ReactAgentAdapter;
import pvt.mktech.petcare.agent.orchestrator.AgentOrchestrator;
import pvt.mktech.petcare.agent.repository.AgentExecutionRepository;

import java.util.List;

/**
 * {@code @description}: Agent 自动配置类
 * {@code @date}: 2026-03-17
 * @author Michael Li
 */
@Configuration
@EnableConfigurationProperties(AgentProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.agent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentAutoConfiguration {

    /**
     * React Agent 原生实例（由 Spring AI Alibaba 构建）
     */
    @Bean
    public ReactAgent reactAgent(ChatModel chatModel, AgentProperties agentProperties, List<ToolCallback> tools) throws Exception {
        ChatClient chatClient = ChatClient.create(chatModel);
        ToolCallbackResolver resolver = new StaticToolCallbackResolver(tools);
        
        return ReactAgent.builder()
                .name("PetCare ReAct Agent")
                .description("宠物关怀 AI 助手，支持多步推理和工具调用")
                .chatClient(chatClient)
                .model(chatModel)
                .tools(tools)
                .resolver(resolver)
                .maxIterations(agentProperties.getMaxIterations())
                .build();
    }

    /**
     * ReAct Agent 适配器（适配我们的 Agent 接口）
     */
    @Bean
    public Agent reactAgentAdapter(
            ReactAgent reactAgent,
            AgentProperties agentProperties,
            AgentExecutionRepository agentExecutionRepository) {
        return new ReactAgentAdapter(reactAgent, agentProperties, agentExecutionRepository);
    }

    /**
     * Agent 调度器
     */
    @Bean
    public AgentOrchestrator agentOrchestrator(
            AgentProperties agentProperties,
            List<Agent> agents,
            ReactAgent reactAgent) {
        return new AgentOrchestrator(agentProperties, agents, reactAgent);
    }
}
