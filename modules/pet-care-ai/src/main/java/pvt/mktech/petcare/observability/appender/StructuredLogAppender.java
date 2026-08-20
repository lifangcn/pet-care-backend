package pvt.mktech.petcare.observability.appender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import pvt.mktech.petcare.common.thread.ThreadPoolManager;
import pvt.mktech.petcare.observability.context.ObservationContext;
import pvt.mktech.petcare.observability.dto.ChatTraceDocument;
import pvt.mktech.petcare.observability.store.ChatTraceStore;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 结构化日志追加器
 * 职责：异步写入链路追踪日志到配置的遥测存储
 *
 * @description: 将 ObservationContext 转换为 ChatTraceDocument 并异步持久化
 * @date: 2026-03-06
 * @author Michael Li
 */
public class StructuredLogAppender {

    private static final Logger log = LoggerFactory.getLogger(StructuredLogAppender.class);

    private final ChatTraceStore chatTraceStore;
    private final TokenCountEstimator tokenCountEstimator;

    /** 异步写入线程池 */
    private static final ThreadPoolExecutor EXECUTOR = ThreadPoolManager.createThreadPool("ObservabilityLogWriter");

    public StructuredLogAppender(ChatTraceStore chatTraceStore, TokenCountEstimator tokenCountEstimator) {
        this.chatTraceStore = chatTraceStore;
        this.tokenCountEstimator = tokenCountEstimator;
    }

    /**
     * 异步追加日志
     */
    public void appendAsync(ObservationContext context) {
        try {
            EXECUTOR.execute(() -> {
                try {
                    write(context);
                    log.debug("[可观测性] 日志写入完成: traceId={}", context.getTraceId());
                } catch (Exception e) {
                    log.error("写入可观测性日志失败: traceId={}", context.getTraceId(), e);
                    fallbackLog(context);
                }
            });
        } catch (RejectedExecutionException e) {
            log.error("提交可观测性日志任务失败: traceId={}", context.getTraceId(), e);
            fallbackLog(context);
        }
    }

    /**
     * 同步追加日志
     */
    private void write(ObservationContext context) {
        estimateTokens(context);
        ChatTraceDocument document = toDocument(context);
        chatTraceStore.save(document);
    }

    /**
     * Token 估算
     */
    private void estimateTokens(ObservationContext context) {
        String requestContent = Objects.requireNonNullElse(context.getRequestContent(), "");
        if (!requestContent.isEmpty()) {
            context.setRequestTokens(tokenCountEstimator.estimate(requestContent));
        }
        String responseText = context.getResponseContent() == null ? "" : context.getResponseContent().toString();
        if (!responseText.isEmpty()) {
            context.setResponseTokens(tokenCountEstimator.estimate(responseText));
        }
    }

    /**
     * 转换为链路追踪文档
     */
    ChatTraceDocument toDocument(ObservationContext context) {
        ChatTraceDocument doc = new ChatTraceDocument();
        doc.setTraceId(context.getTraceId());
        doc.setConversationId(context.getConversationId());
        doc.setUserId(context.getUserId());
        doc.setSessionId(context.getSessionId());
        doc.setTimestamp(context.getStartTime());
        doc.setDurationMs(context.getDurationMs());

        // 请求信息
        ChatTraceDocument.RequestInfo request = new ChatTraceDocument.RequestInfo();
        request.setContent(Objects.requireNonNullElse(context.getRequestContent(), ""));
        request.setTokens(context.getRequestTokens());
        doc.setRequest(request);

        // 响应信息
        ChatTraceDocument.ResponseInfo response = new ChatTraceDocument.ResponseInfo();
        response.setContent(context.getResponseContent() == null ? "" : context.getResponseContent().toString());
        response.setTokens(context.getResponseTokens());
        response.setFinishReason(context.getFinishReason());
        doc.setResponse(response);

        // RAG 信息
        if (context.getRagInfo() != null) {
            doc.setRag(toRagDocument(context.getRagInfo()));
        }

        // Tool Calls
        List<ObservationContext.ToolCallInfo> toolCalls = Objects.requireNonNullElse(context.getToolCalls(), List.of());
        doc.setToolCalls(toolCalls.stream().map(this::toToolCallDocument).toList());

        // 错误信息
        if (context.getErrorInfo() != null) {
            doc.setError(toErrorDocument(context.getErrorInfo()));
        }

        // 元数据
        ChatTraceDocument.Metadata metadata = new ChatTraceDocument.Metadata();
        metadata.setModel(context.getModel());
        doc.setMetadata(metadata);

        return doc;
    }

    private ChatTraceDocument.RAGInfo toRagDocument(ObservationContext.RAGInfo ragInfo) {
        ChatTraceDocument.RAGInfo doc = new ChatTraceDocument.RAGInfo();
        doc.setEnabled(ragInfo.isEnabled());
        doc.setQuery(ragInfo.getQuery());
        doc.setResultsCount(ragInfo.getResultsCount());
        doc.setTopScore(ragInfo.getTopScore());
        doc.setDurationMs(ragInfo.getDurationMs());
        return doc;
    }

    private ChatTraceDocument.ToolCallInfo toToolCallDocument(ObservationContext.ToolCallInfo info) {
        ChatTraceDocument.ToolCallInfo doc = new ChatTraceDocument.ToolCallInfo();
        doc.setToolName(info.getToolName());
        doc.setArguments(info.getArguments());
        doc.setResult(info.getResult());
        doc.setDurationMs(info.getDurationMs());
        doc.setSuccess(info.isSuccess());
        return doc;
    }

    private ChatTraceDocument.ErrorInfo toErrorDocument(ObservationContext.ErrorInfo errorInfo) {
        ChatTraceDocument.ErrorInfo doc = new ChatTraceDocument.ErrorInfo();
        doc.setType(errorInfo.getType());
        doc.setMessage(errorInfo.getMessage());
        doc.setStackTrace(errorInfo.getStackTrace());
        return doc;
    }

    /**
     * 降级策略：写入本地日志
     */
    protected void fallbackLog(ObservationContext context) {
        log.warn("可观测性存储失败，降级到本地日志: traceId={}, userId={}, durationMs={}",
                context.getTraceId(), context.getUserId(), context.getDurationMs());
    }
}
