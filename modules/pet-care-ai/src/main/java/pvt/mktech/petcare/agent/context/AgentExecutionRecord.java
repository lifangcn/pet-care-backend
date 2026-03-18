package pvt.mktech.petcare.agent.context;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * {@code @description}: Agent 执行记录
 * {@code @date}: 2026-03-17
 * @author Michael Li
 */
@Data
@Builder
public class AgentExecutionRecord {

    /**
     * 执行 ID
     */
    private String executionId;

    /**
     * Agent 类型
     */
    private String agentType;

    /**
     * 会话 ID
     */
    private String conversationId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户查询
     */
    private String query;

    /**
     * 执行步骤列表
     */
    private List<AgentStep> steps;

    /**
     * 最终答案
     */
    private String finalAnswer;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 失败原因
     */
    private String reason;

    /**
     * 总步数
     */
    private int totalSteps;

    /**
     * 工具调用次数
     */
    private int toolCalls;

    /**
     * 总耗时（毫秒）
     */
    private long totalDurationMs;

    /**
     * 创建时间
     */
    private Instant createdAt;
}
