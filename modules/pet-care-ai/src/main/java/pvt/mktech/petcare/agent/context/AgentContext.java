package pvt.mktech.petcare.agent.context;

import lombok.Builder;
import lombok.Data;

/**
 * {@code @description}: Agent 执行上下文
 * {@code @date}: 2026-03-17
 * @author Michael Li
 */
@Data
@Builder
public class AgentContext {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 会话 ID
     */
    private String conversationId;

    /**
     * 执行 ID（唯一标识一次 Agent 执行）
     */
    private String executionId;
}
