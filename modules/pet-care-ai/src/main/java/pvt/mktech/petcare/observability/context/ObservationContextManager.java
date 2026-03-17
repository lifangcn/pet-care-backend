package pvt.mktech.petcare.observability.context;

/**
 * 追踪上下文管理器
 * 职责：管理 ThreadLocal 生命周期
 *
 * @description: 管理 ObservationContext 的 ThreadLocal 存储，提供创建、获取、清理方法
 * @date: 2026-03-06
 * @author Michael Li
 */
public class ObservationContextManager {

    private static final ThreadLocal<ObservationContext> CONTEXT = new ThreadLocal<>();

    /**
     * 创建并设置上下文
     *
     * @param traceId        链路ID
     * @param conversationId 对话ID
     * @param userId         用户ID
     * @param sessionId      会话ID
     * @return 创建的上下文实例
     */
    public static ObservationContext create(String traceId, String conversationId,
                                           Long userId, String sessionId) {
        ObservationContext context = new ObservationContext();
        context.setTraceId(traceId);
        context.setConversationId(conversationId);
        context.setUserId(userId);
        context.setSessionId(sessionId);
        context.setStartTime(java.time.Instant.now());
        CONTEXT.set(context);
        return context;
    }

    /**
     * 获取当前上下文
     *
     * @return 当前上下文，如果不存在返回 null
     */
    public static ObservationContext get() {
        return CONTEXT.get();
    }

    /**
     * 清理上下文（必须在 finally 中调用）
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 判断是否存在上下文
     *
     * @return 是否存在上下文
     */
    public static boolean exists() {
        return CONTEXT.get() != null;
    }
}
