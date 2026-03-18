package pvt.mktech.petcare.agent.context;

import lombok.Builder;
import lombok.Data;

/**
 * {@code @description}: Agent 执行步骤记录
 * {@code @date}: 2026-03-17
 * @author Michael Li
 */
@Data
@Builder
public class AgentStep {

    /**
     * 步骤序号
     */
    private int stepNumber;

    /**
     * 思考内容
     */
    private String thought;

    /**
     * 动作类型
     */
    private String action;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 工具输入
     */
    private String toolInput;

    /**
     * 观察结果（工具返回）
     */
    private String observation;

    /**
     * 耗时（毫秒）
     */
    private long durationMs;
}
