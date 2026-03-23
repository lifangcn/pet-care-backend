package pvt.mktech.petcare.agent.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
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
    public ReactAgent reactAgent(DashScopeChatModel dashScopeChatModel, AgentProperties agentProperties, List<ToolCallback> tools) throws Exception {
        ChatClient chatClient = ChatClient.create(dashScopeChatModel);
        ToolCallbackResolver resolver = new StaticToolCallbackResolver(tools);
        
        String instruction = """
                你是宠物关怀平台的 AI 助手，负责帮用户查找活动、帖子、知识等信息。
                
                你拥有以下工具，可以在回答问题时使用：
                1. calculateDateRange - 当用户提到"周末"、"下周"、"明天"等模糊时间时，必须先调用此工具解析日期范围
                2. rerankByRating - 查找结果后调用此工具按评分排序
                3. filterResultsByKeyword - 按关键词和分数过滤结果
                
                工作流程：
                1. 理解用户问题，如果涉及模糊时间，先调用 calculateDateRange 获取具体日期范围
                2. 使用获取到的日期范围去数据库查找相关活动/帖子
                3. 查找完成后，根据需要调用 rerankByRating 或 filterResultsByKeyword 处理结果
                4. 最后整理结果回答用户
                
                注意：不要直接说你无法获取信息，你有工具可以帮助用户，请先使用工具。
                """;
        
        return ReactAgent.builder()
                .name("PetCare ReAct Agent")
                .description("宠物关怀 AI 助手，支持多步推理和工具调用")
                .instruction(instruction)
                .chatClient(chatClient)
                .model(dashScopeChatModel)
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
