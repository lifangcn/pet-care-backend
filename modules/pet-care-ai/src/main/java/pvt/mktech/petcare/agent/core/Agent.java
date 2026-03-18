package pvt.mktech.petcare.agent.core;

import pvt.mktech.petcare.agent.context.AgentContext;
import reactor.core.publisher.Flux;

/**
 * {@code @description}: Agent 接口
 * {@code @date}: 2026-03-17
 * @author Michael Li
 */
public interface Agent {

    /**
     * 获取 Agent 类型
     */
    String getType();

    /**
     * 执行 Agent（流式响应）
     * @param query 用户查询
     * @param context 执行上下文
     * @return 流式输出（步骤事件 + 最终答案）
     */
    Flux<String> executeStreaming(String query, AgentContext context);
}
