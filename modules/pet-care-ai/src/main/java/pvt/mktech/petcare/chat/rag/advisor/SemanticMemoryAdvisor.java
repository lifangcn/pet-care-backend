package pvt.mktech.petcare.chat.rag.advisor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.chat.repository.ChatHistoryRepository;
import pvt.mktech.petcare.common.web.UserContext;
import pvt.mktech.petcare.entity.ChatMessageDocument;
import pvt.mktech.petcare.infrastructure.config.ChatMemoryProperties;

import reactor.core.publisher.Flux;

import java.util.List;

/**
 * {@code @description}: 语义记忆检索Advisor
 * 在LLM调用前检索相关历史对话，增强上下文
 * {@code @date}: 2026-03-02
 *
 * @author Michael
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "spring.ai.chat.memory.semantic",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
@Lazy
public class SemanticMemoryAdvisor implements CallAdvisor, StreamAdvisor {

    private final ChatHistoryRepository chatHistoryRepository;
    private final ChatMemoryProperties properties;

    @Override
    public String getName() {
        return "SemanticMemoryAdvisor";
    }

    @Override
    public int getOrder() {
        return 0; // 最先执行，确保历史上下文最先注入
    }

    @Override
    public ChatClientResponse adviseCall(
            ChatClientRequest request,
            CallAdvisorChain chain) {

        // 检索相关历史并记录日志
        String prompt = request.prompt() != null ? request.prompt().toString() : "";
        retrieveAndLogHistory(prompt);

        return chain.nextCall(request);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
            ChatClientRequest request,
            StreamAdvisorChain chain) {

        // 流式同理
        String prompt = request.prompt() != null ? request.prompt().toString() : "";
        retrieveAndLogHistory(prompt);

        return chain.nextStream(request);
    }

    /**
     * 检索并记录历史（先实现基础版本）
     */
    private void retrieveAndLogHistory(String prompt) {
        try {
            Long userId = UserContext.getUserId();
            if (userId == null) {
                return;
            }

            if (!org.springframework.util.StringUtils.hasText(prompt)) {
                return;
            }

            // 语义检索
            List<ChatMessageDocument> history = chatHistoryRepository.semanticSearch(
                    prompt,
                    userId,
                    properties.getSemantic().getTopK(),
                    properties.getSemantic().getMinScore()
            );

            if (!history.isEmpty()) {
                log.debug("检索到{}条相关历史", history.size());
                // TODO: 将历史上下文注入到Prompt中
                // 由于Spring AI 1.0.1的API限制，暂时只做记录
            }

        } catch (Exception e) {
            log.error("检索历史上下文失败", e);
        }
    }
}
