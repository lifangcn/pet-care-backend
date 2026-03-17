package pvt.mktech.petcare.observability.appender;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import pvt.mktech.petcare.common.thread.ThreadPoolManager;
import pvt.mktech.petcare.observability.context.ObservationContext;
import pvt.mktech.petcare.observability.dto.ChatTraceDocument;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 结构化日志追加器
 * 职责：异步写入链路追踪日志到 ES
 *
 * @description: 将 ObservationContext 转换为 ChatTraceDocument 并异步写入 Elasticsearch
 * @date: 2026-03-06
 * @author Michael Li
 */
@Slf4j
@RequiredArgsConstructor
public class StructuredLogAppender {

    private final ElasticsearchClient elasticsearchClient;
    private final TokenCountEstimator tokenCountEstimator;
    private final String indexName;

    /** 异步写入线程池 */
    private static final ThreadPoolExecutor EXECUTOR = ThreadPoolManager.createThreadPool("ObservabilityLogWriter");

    /**
     * 异步追加日志
     */
    public void appendAsync(ObservationContext context) {
        EXECUTOR.execute(() -> {
            try {
                append(context);
                log.debug("[可观测性] 日志写入完成: traceId={}", context.getTraceId());
            } catch (Exception e) {
                log.error("写入可观测性日志失败: traceId={}", context.getTraceId(), e);
                fallbackLog(context);
            }
        });
    }

    /**
     * 同步追加日志
     */
    private void append(ObservationContext context) {
        try {
            // 1. 计算 Token
            estimateTokens(context);

            // 2. 转换为 ES 文档
            ChatTraceDocument document = toDocument(context);

            // 3. 写入 ES
            IndexRequest<ChatTraceDocument> request = IndexRequest.of(i -> i
                    .index(indexName)
                    .id(document.getTraceId())
                    .document(document)
            );

            IndexResponse response = elasticsearchClient.index(request);
            log.debug("可观测性日志已写入: traceId={}, result={}",
                    context.getTraceId(), response.result());

        } catch (Exception e) {
            log.error("写入 ES 失败，尝试降级: traceId={}", context.getTraceId(), e);
            fallbackLog(context);
        }
    }

    /**
     * Token 估算
     */
    private void estimateTokens(ObservationContext context) {
        try {
            if (context.getRequestContent() != null) {
                context.setRequestTokens(tokenCountEstimator.estimate(context.getRequestContent()));
            }

            String responseText = context.getResponseContent().toString();
            if (!responseText.isEmpty()) {
                context.setResponseTokens(tokenCountEstimator.estimate(responseText));
            }
        } catch (Exception e) {
            log.warn("Token 估算失败: traceId={}", context.getTraceId(), e);
        }
    }

    /**
     * 转换为 ES 文档
     */
    private ChatTraceDocument toDocument(ObservationContext context) {
        ChatTraceDocument doc = new ChatTraceDocument();
        doc.setTraceId(context.getTraceId());
        doc.setConversationId(context.getConversationId());
        doc.setUserId(context.getUserId());
        doc.setSessionId(context.getSessionId());
        doc.setTimestamp(context.getStartTime());
        doc.setDurationMs(context.getDurationMs());

        // 请求信息
        ChatTraceDocument.RequestInfo request = new ChatTraceDocument.RequestInfo();
        request.setContent(context.getRequestContent());
        request.setTokens(context.getRequestTokens());
        doc.setRequest(request);

        // 响应信息
        ChatTraceDocument.ResponseInfo response = new ChatTraceDocument.ResponseInfo();
        response.setContent(context.getResponseContent().toString());
        response.setTokens(context.getResponseTokens());
        response.setFinishReason(context.getFinishReason());
        doc.setResponse(response);

        // RAG 信息
        if (context.getRagInfo() != null) {
            doc.setRag(toRagDocument(context.getRagInfo()));
        }

        // Tool Calls
        if (!context.getToolCalls().isEmpty()) {
            doc.setToolCalls(context.getToolCalls().stream()
                    .map(this::toToolCallDocument)
                    .toList());
        }

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
    private void fallbackLog(ObservationContext context) {
        log.warn("ES 写入失败，降级到本地日志: traceId={}, userId={}, durationMs={}",
                context.getTraceId(), context.getUserId(), context.getDurationMs());
    }
}
