package pvt.mktech.petcare.observability.advisor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import pvt.mktech.petcare.common.web.UserContext;
import pvt.mktech.petcare.observability.appender.StructuredLogAppender;
import pvt.mktech.petcare.observability.context.ObservationContext;
import pvt.mktech.petcare.observability.context.ObservationContextManager;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * 可观测性 Advisor
 * 职责：追踪 AI 对话的完整链路，记录请求、响应、RAG、Tool Calling 等信息
 *
 * @description: 实现 CallAdvisor 和 StreamAdvisor，追踪 AI 对话链路，收集数据并异步写入 ES
 * @date: 2026-03-06
 * @author Michael Li
 */
@Slf4j
@RequiredArgsConstructor
public class ObservabilityAdvisor implements CallAdvisor, StreamAdvisor {

    private final StructuredLogAppender structuredLogAppender;

    @Override
    public String getName() {
        return "ObservabilityAdvisor";
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ObservationContext context = createContext(request);
        if (context == null) {
            return chain.nextCall(request);
        }

        Object prompt = request.prompt();
        context.setRequestContent(prompt != null ? prompt.toString() : "");

        try {
            ChatClientResponse response = chain.nextCall(request);

            String responseText = response.chatResponse().getResult().getOutput().getText();
            context.getResponseContent().append(responseText);
            context.setFinishReason("stop");

            submitLog(context);
            return response;
        } catch (Exception e) {
            handleError(context, e);
            throw e;
        } finally {
            ObservationContextManager.clear();
        }
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        ObservationContext context = createContext(request);
        if (context == null) {
            return chain.nextStream(request);
        }

        Object prompt = request.prompt();
        context.setRequestContent(prompt != null ? prompt.toString() : "");

        Flux<ChatClientResponse> responseFlux = chain.nextStream(request);

        return new ChatClientMessageAggregator().aggregateChatClientResponse(
                responseFlux,
                chatClientResponse -> {
                    try {
                        String responseText = chatClientResponse.chatResponse().getResult().getOutput().getText();
                        context.getResponseContent().append(responseText);
                        context.setFinishReason("stop");
                        submitLog(context);
                    } catch (Exception e) {
                        handleError(context, e);
                    } finally {
                        ObservationContextManager.clear();
                    }
                }
        );
    }

    private ObservationContext createContext(ChatClientRequest request) {
        if (structuredLogAppender == null) {
            log.debug("[可观测性] StructuredLogAppender 未注入，跳过追踪");
            return null;
        }

        try {
            Long userId = UserContext.getUserId();
            String traceId = generateTraceId();
            String conversationId = extractParam(request, "CONVERSATION_ID", "unknown");
            String sessionId = extractParam(request, "sessionId", null);

            ObservationContext context = ObservationContextManager.create(
                    traceId, conversationId, userId, sessionId);

            log.debug("[可观测性] 创建追踪上下文: traceId={}, conversationId={}, sessionId={}", 
                    traceId, conversationId, sessionId);
            return context;
        } catch (Exception e) {
            log.warn("创建追踪上下文失败", e);
            return null;
        }
    }

    private String extractParam(ChatClientRequest request, String key, String defaultValue) {
        if (request.context() == null) {
            return defaultValue;
        }
        Object value = request.context().get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void submitLog(ObservationContext context) {
        if (structuredLogAppender == null) {
            log.debug("[可观测性] StructuredLogAppender 为空，无法提交日志: traceId={}", context.getTraceId());
            return;
        }
        try {
            context.setDurationMs((int) Duration.between(context.getStartTime(), Instant.now()).toMillis());
            structuredLogAppender.appendAsync(context);
        } catch (Exception e) {
            log.error("提交可观测性日志失败: traceId={}", context.getTraceId(), e);
        }
    }

    private void handleError(ObservationContext context, Exception e) {
        if (context != null) {
            ObservationContext.ErrorInfo errorInfo = new ObservationContext.ErrorInfo();
            errorInfo.setType(e.getClass().getSimpleName());
            errorInfo.setMessage(e.getMessage());
            context.setErrorInfo(errorInfo);
            context.setFinishReason("error");
            submitLog(context);
        }
    }
}
