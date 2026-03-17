package pvt.mktech.petcare.observability.context;

import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 链路追踪上下文
 * ThreadLocal 存储，一次请求一个实例
 *
 * @description: 存储一次 AI 对话请求的完整追踪信息，包括请求、响应、RAG 检索、工具调用等
 * @date: 2026-03-06
 * @author Michael Li
 */
@Data
public class ObservationContext {

    // ========== 基础信息 ==========
    /** 链路唯一标识（UUID） */
    private String traceId;

    /** 对话ID */
    private String conversationId;

    /** 用户ID */
    private Long userId;

    /** 会话标识 */
    private String sessionId;

    /** 请求开始时间 */
    private Instant startTime;

    /** 模型名称 */
    private String model;

    /** 总耗时（毫秒） */
    private Integer durationMs;

    // ========== 请求信息 ==========
    /** 用户请求内容 */
    private String requestContent;

    /** 请求 Token 数量 */
    private Integer requestTokens;

    // ========== 响应信息 ==========
    /** AI 响应内容（流式累加） */
    private StringBuilder responseContent;

    /** 响应 Token 数量 */
    private Integer responseTokens;

    /** 结束原因（stop/length/cancelled） */
    private String finishReason;

    // ========== RAG 检索信息 ==========
    /** RAG 检索详情 */
    private RAGInfo ragInfo;

    // ========== Tool 调用信息 ==========
    /** 工具调用列表（可能多个） */
    private List<ToolCallInfo> toolCalls;

    // ========== 错误信息 ==========
    /** 错误详情 */
    private ErrorInfo errorInfo;

    public ObservationContext() {
        this.responseContent = new StringBuilder();
        this.toolCalls = new ArrayList<>();
    }

    /**
     * RAG 检索信息
     */
    @Data
    public static class RAGInfo {
        /** 是否启用 RAG */
        private boolean enabled;

        /** 检索查询 */
        private String query;

        /** 检索结果数量 */
        private int resultsCount;

        /** 最高分数 */
        private double topScore;

        /** 检索耗时（毫秒） */
        private long durationMs;
    }

    /**
     * 工具调用信息
     */
    @Data
    public static class ToolCallInfo {
        /** 工具名称 */
        private String toolName;

        /** 调用参数 */
        private String arguments;

        /** 调用结果 */
        private String result;

        /** 调用耗时（毫秒） */
        private long durationMs;

        /** 是否成功 */
        private boolean success;

        /** 开始时间 */
        private Instant startTime;
    }

    /**
     * 错误信息
     */
    @Data
    public static class ErrorInfo {
        /** 错误类型 */
        private String type;

        /** 错误消息 */
        private String message;

        /** 堆栈跟踪 */
        private String stackTrace;
    }
}
