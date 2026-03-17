package pvt.mktech.petcare.chat.rag.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import pvt.mktech.petcare.observability.context.ObservationContext;
import pvt.mktech.petcare.observability.context.ObservationContextManager;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;

/**
 * {@code @description}: 日志 Advisor
 * 职责：记录请求/响应日志，追踪 Tool Calling
 * {@code @date}: 2026/1/7 11:35
 *
 * @author Michael
 */
@Slf4j
public class MyLoggerAdvisor implements CallAdvisor, StreamAdvisor {

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain chain) {
        log.info("AI Request: {}", chatClientRequest.prompt());
        ChatClientResponse chatClientResponse = chain.nextCall(chatClientRequest);
        log.info("AI Response: {}", chatClientResponse.chatResponse().getResult().getOutput().getText());

        // 提取 Tool Calling 信息
        extractToolCalls(chatClientResponse.chatResponse());

        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain chain) {
        log.info("AI Request: {}", chatClientRequest.prompt());
        Flux<ChatClientResponse> chatClientResponseFlux = chain.nextStream(chatClientRequest);

        return new ChatClientMessageAggregator().aggregateChatClientResponse(chatClientResponseFlux,
                chatClientResponse -> {
                    log.info("AI Response: {}", chatClientResponse.chatResponse().getResult().getOutput().getText());
                    // 提取 Tool Calling 信息
                    extractToolCalls(chatClientResponse.chatResponse());
                });
    }

    /**
     * 提取 Tool Calling 信息并写入 ObservationContext
     * 注意：Spring AI 1.0.1 的 Tool Calling 返回结构可能需要根据实际情况调整
     */
    private void extractToolCalls(ChatResponse chatResponse) {
        if (!ObservationContextManager.exists()) {
            return;
        }

        try {
            List<Generation> generations = chatResponse.getResults();
            if (generations == null || generations.isEmpty()) {
                return;
            }

            Generation generation = generations.get(0);

            // 检查是否有工具调用（Spring AI 1.0.1 中 toolCalls 在 metadata 中）
            if (generation.getMetadata() != null && generation.getMetadata().containsKey("toolCalls")) {
                @SuppressWarnings("unchecked")
                List<com.fasterxml.jackson.databind.node.ObjectNode> toolCalls =
                        (List<com.fasterxml.jackson.databind.node.ObjectNode>) generation.getMetadata().get("toolCalls");

                if (toolCalls != null && !toolCalls.isEmpty()) {
                    ObservationContext context = ObservationContextManager.get();

                    for (var toolCall : toolCalls) {
                        ObservationContext.ToolCallInfo info = new ObservationContext.ToolCallInfo();

                        // 提取工具名称
                        if (toolCall.has("name")) {
                            info.setToolName(toolCall.get("name").asText());
                        }

                        // 提取参数
                        if (toolCall.has("arguments")) {
                            info.setArguments(toolCall.get("arguments").toString());
                        }

                        info.setStartTime(Instant.now());
                        info.setSuccess(true);
                        info.setResult("Tool executed");

                        context.getToolCalls().add(info);

                        log.info("Tool Call: {} | Arguments: {}", info.getToolName(), info.getArguments());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("提取 Tool Calling 信息失败（可能无工具调用）", e);
        }
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
