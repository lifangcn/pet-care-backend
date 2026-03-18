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
import java.lang.reflect.Field;

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

        // 检索相关历史并注入到prompt
        String originalPrompt = request.prompt() != null ? request.prompt().toString() : "";
        List<ChatMessageDocument> relatedHistory = retrieveRelatedHistory(originalPrompt);
        
        if (!relatedHistory.isEmpty()) {
            // 构建增强prompt
            String enhancedPrompt = buildEnhancedPrompt(originalPrompt, relatedHistory);
            // 通过反射修改prompt（Spring AI 1.0.1 API限制下的方案）
            updateRequestPrompt(request, enhancedPrompt);
        }

        return chain.nextCall(request);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
            ChatClientRequest request,
            StreamAdvisorChain chain) {

        // 流式同理
        String originalPrompt = request.prompt() != null ? request.prompt().toString() : "";
        List<ChatMessageDocument> relatedHistory = retrieveRelatedHistory(originalPrompt);
        
        if (!relatedHistory.isEmpty()) {
            String enhancedPrompt = buildEnhancedPrompt(originalPrompt, relatedHistory);
            updateRequestPrompt(request, enhancedPrompt);
        }

        return chain.nextStream(request);
    }

    /**
     * 检索相关历史对话
     */
    private List<ChatMessageDocument> retrieveRelatedHistory(String prompt) {
        try {
            Long userId = UserContext.getUserId();
            if (userId == null || !org.springframework.util.StringUtils.hasText(prompt)) {
                return List.of();
            }

            // 语义检索
            List<ChatMessageDocument> history = chatHistoryRepository.semanticSearch(
                    prompt,
                    userId,
                    properties.getSemantic().getTopK(),
                    properties.getSemantic().getMinScore()
            );

            if (!history.isEmpty()) {
                log.debug("检索到{}条相关历史对话", history.size());
            }
            return history;

        } catch (Exception e) {
            log.error("检索历史上下文失败", e);
            return List.of();
        }
    }

    /**
     * 构建增强prompt，注入相关历史上下文
     */
    private String buildEnhancedPrompt(String originalPrompt, List<ChatMessageDocument> relatedHistory) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是与当前问题相关的历史对话上下文，请参考这些信息来回答用户的问题：\n\n");
        
        for (ChatMessageDocument doc : relatedHistory) {
            sb.append("[").append(doc.getRole()).append("]: ").append(doc.getContent()).append("\n");
        }
        
        sb.append("\n当前用户问题：\n").append(originalPrompt);
        return sb.toString();
    }

    /**
     * 通过反射修改ChatClientRequest中的prompt字段
     * 适配Spring AI 1.0.1 API
     */
    private void updateRequestPrompt(ChatClientRequest request, String newPrompt) {
        try {
            Field promptField = ChatClientRequest.class.getDeclaredField("prompt");
            promptField.setAccessible(true);
            promptField.set(request, newPrompt);
            log.debug("成功注入语义记忆上下文到prompt");
        } catch (Exception e) {
            log.error("注入语义记忆上下文失败", e);
        }
    }
}
