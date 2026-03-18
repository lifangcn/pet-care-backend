package pvt.mktech.petcare.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code @description}: Agent 配置属性
 * {@code @date}: 2026-03-17
 * @author Michael Li
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.agent")
public class AgentProperties {

    /**
     * 是否启用 Agent 功能
     */
    private boolean enabled = true;

    /**
     * 默认 Agent 类型
     */
    private AgentType defaultType = AgentType.REACT;

    /**
     * 最大迭代次数（ReAct）
     */
    private int maxIterations = 5;

    /**
     * 超时时间（毫秒）
     */
    private long timeoutMs = 30000;

    /**
     * ReAct Prompt 模板路径
     */
    private String promptTemplate = "react/default";

    /**
     * Agent 类型枚举
     */
    public enum AgentType {
        SIMPLE,
        REACT,
        PLAN_AND_EXECUTE
    }
}
