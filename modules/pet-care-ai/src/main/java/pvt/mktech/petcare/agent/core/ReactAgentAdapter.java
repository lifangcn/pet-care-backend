package pvt.mktech.petcare.agent.core;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import pvt.mktech.petcare.agent.config.AgentProperties;
import pvt.mktech.petcare.agent.context.AgentContext;
import pvt.mktech.petcare.agent.context.AgentExecutionRecord;
import pvt.mktech.petcare.agent.context.AgentStep;
import pvt.mktech.petcare.agent.repository.AgentExecutionRepository;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code @description}: ReAct Agent 适配器 - 适配 Spring AI Alibaba Graph 的 ReactAgent
 * {@code @date}: 2026-03-17
 * @author Michael Li
 */
@Slf4j
@RequiredArgsConstructor
public class ReactAgentAdapter implements Agent {

    private final ReactAgent reactAgent;
    private final AgentProperties agentProperties;
    private final AgentExecutionRepository executionRepository;

    private volatile CompiledGraph compiledGraph;

    /**
     * 懒初始化编译后的 Graph
     */
    private void initCompiledGraph() throws GraphStateException {
        if (compiledGraph == null) {
            synchronized (this) {
                if (compiledGraph == null) {
                    compiledGraph = reactAgent.getAndCompileGraph();
                    log.info("ReactAgent Graph 编译完成");
                }
            }
        }
    }

    @Override
    public String getType() {
        return AgentProperties.AgentType.REACT.name();
    }

    @Override
    public Flux<String> executeStreaming(String query, AgentContext context) {
        return Flux.create(sink -> {
            long startTime = System.currentTimeMillis();
            String executionId = UUID.randomUUID().toString();
            context.setExecutionId(executionId);
            try {
                initCompiledGraph();

                // Graph does not currently expose tool/intermediate state; persist no synthetic steps.
                List<AgentStep> steps = List.of();

                var result = compiledGraph.call(Map.of("messages", List.of(new UserMessage(query))));

                if (result.isEmpty()) {
                    throw new IllegalStateException("Agent 执行返回空结果");
                }

                OverAllState state = result.get();
                List<Message> messages = state.value("messages")
                        .filter(List.class::isInstance)
                        .map(value -> ((List<?>) value).stream()
                                .filter(Message.class::isInstance)
                                .map(Message.class::cast)
                                .toList())
                        .orElseGet(List::of);

                for (var message : messages) {
                    if (message instanceof org.springframework.ai.chat.messages.AssistantMessage assistantMsg) {
                        String content = assistantMsg.getText();
                        if (content != null && !content.isEmpty()) {
                            sink.next(formatStepEvent("answer", content));
                        }
                    }
                }

                String finalAnswer = "";
                if (!messages.isEmpty()) {
                    var lastMessage = messages.get(messages.size() - 1);
                    if (lastMessage instanceof org.springframework.ai.chat.messages.AssistantMessage assistantMsg) {
                        String text = assistantMsg.getText();
                        finalAnswer = (text != null && !text.isEmpty()) ? text : "[Agent 执行完成，无文本输出]";
                    }
                }

                saveExecutionRecord(context, query, steps, finalAnswer, true, null, startTime);

                sink.complete();

            } catch (GraphStateException | RuntimeException e) {
                log.error("ReAct Agent 执行失败: query={}", query, e);
                saveExecutionRecord(context, query, List.of(), null, false, e.getMessage(), startTime);
                sink.error(e);
            }
        });
    }

    /**
     * 格式化事件输出
     */
    private String formatStepEvent(String eventType, String content) {
        return String.format("[%s] %s", eventType, content);
    }

    /**
     * 保存执行记录到配置的遥测存储
     */
    private void saveExecutionRecord(AgentContext context, String query,
                                      List<AgentStep> steps, String finalAnswer,
                                      boolean success, String reason, long startTime) {
        try {
            AgentExecutionRecord record = AgentExecutionRecord.builder()
                    .executionId(context.getExecutionId())
                    .agentType(getType())
                    .conversationId(context.getConversationId())
                    .userId(context.getUserId())
                    .query(query)
                    .steps(steps)
                    .finalAnswer(finalAnswer)
                    .success(success)
                    .reason(reason)
                    .totalSteps(steps.size())
                    .toolCalls((int) steps.stream().filter(s -> "TOOL_CALL".equals(s.getAction())).count())
                    .totalDurationMs(System.currentTimeMillis() - startTime)
                    .createdAt(Instant.now())
                    .build();

            executionRepository.save(record);
        } catch (Exception e) {
            log.warn("保存 Agent 执行记录失败: executionId={}", context.getExecutionId(), e);
        }
    }
}
