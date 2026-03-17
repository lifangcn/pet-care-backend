package pvt.mktech.petcare.observability.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 聊天链路追踪 ES 文档实体
 * 对应索引：chat_trace
 *
 * @description: Elasticsearch 文档实体，用于存储 AI 对话链路追踪信息
 * @date: 2026-03-06
 * @author Michael Li
 */
@Data
public class ChatTraceDocument {

    /** 链路唯一标识 */
    @JsonProperty("trace_id")
    private String traceId;

    /** 对话ID */
    @JsonProperty("conversation_id")
    private String conversationId;

    /** 会话标识 */
    @JsonProperty("session_id")
    private String sessionId;

    /** 用户ID */
    @JsonProperty("user_id")
    private Long userId;

    /** 时间戳 */
    @JsonProperty("timestamp")
    private Instant timestamp;

    /** 总耗时（毫秒） */
    @JsonProperty("duration_ms")
    private Integer durationMs;

    /** 请求信息 */
    @JsonProperty("request")
    private RequestInfo request;

    /** 响应信息 */
    @JsonProperty("response")
    private ResponseInfo response;

    /** RAG 检索信息 */
    @JsonProperty("rag")
    private RAGInfo rag;

    /** 工具调用列表 */
    @JsonProperty("tool_calls")
    private List<ToolCallInfo> toolCalls;

    /** 错误信息 */
    @JsonProperty("error")
    private ErrorInfo error;

    /** 元数据 */
    @JsonProperty("metadata")
    private Metadata metadata;

    /**
     * 请求信息
     */
    @Data
    public static class RequestInfo {
        /** 请求内容 */
        @JsonProperty("content")
        private String content;

        /** Token 数量 */
        @JsonProperty("tokens")
        private Integer tokens;
    }

    /**
     * 响应信息
     */
    @Data
    public static class ResponseInfo {
        /** 响应内容 */
        @JsonProperty("content")
        private String content;

        /** Token 数量 */
        @JsonProperty("tokens")
        private Integer tokens;

        /** 结束原因 */
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    /**
     * RAG 检索信息
     */
    @Data
    public static class RAGInfo {
        /** 是否启用 */
        @JsonProperty("enabled")
        private boolean enabled;

        /** 检索查询 */
        @JsonProperty("query")
        private String query;

        /** 结果数量 */
        @JsonProperty("results_count")
        private int resultsCount;

        /** 最高分数 */
        @JsonProperty("top_score")
        private double topScore;

        /** 检索耗时（毫秒） */
        @JsonProperty("duration_ms")
        private long durationMs;
    }

    /**
     * 工具调用信息
     */
    @Data
    public static class ToolCallInfo {
        /** 工具名称 */
        @JsonProperty("tool_name")
        private String toolName;

        /** 调用参数 */
        @JsonProperty("arguments")
        private String arguments;

        /** 调用结果 */
        @JsonProperty("result")
        private String result;

        /** 调用耗时（毫秒） */
        @JsonProperty("duration_ms")
        private long durationMs;

        /** 是否成功 */
        @JsonProperty("success")
        private boolean success;
    }

    /**
     * 错误信息
     */
    @Data
    public static class ErrorInfo {
        /** 错误类型 */
        @JsonProperty("type")
        private String type;

        /** 错误消息 */
        @JsonProperty("message")
        private String message;

        /** 堆栈跟踪 */
        @JsonProperty("stack_trace")
        private String stackTrace;
    }

    /**
     * 元数据
     */
    @Data
    public static class Metadata {
        /** 模型名称 */
        @JsonProperty("model")
        private String model;

        /** 客户端IP */
        @JsonProperty("ip")
        private String ip;

        /** User Agent */
        @JsonProperty("user_agent")
        private String userAgent;
    }
}
