package pvt.mktech.petcare.infrastructure.config;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * ChatModel 装饰器，确保 AssistantMessage.getText() 永不为 null。
 * <p>
 * 修复 Spring AI Alibaba NodeExecutor 在使用思考模型（如 DeepSeek deepseek-v4-flash）
 * 时返回 reasoning_content 而非常规 text 导致的 NPE：
 * {@code requireNonNull(lastResponse.getResult().getOutput().getText(), "lastResponse text cannot be null")}
 * <p>
 * 思考模型的流式响应中，某些 chunk 仅包含 reasoning_content（思考过程）而 text 为 null，
 * NodeExecutor 在累积文本时调用 getText() 触发 NPE。本装饰器将 null text 替换为空字符串，
 * 不影响正常文本累积逻辑（空字符串拼接等价于跳过）。
 *
 * @author Michael Li
 * @since 2026-06-01
 */
public class NonNullTextChatModelDecorator implements ChatModel {

    private final ChatModel delegate;

    public NonNullTextChatModelDecorator(ChatModel delegate) {
        this.delegate = delegate;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        return sanitizeResponse(delegate.call(prompt));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return delegate.stream(prompt).map(this::sanitizeResponse);
    }

    /**
     * 将 ChatResponse 中 AssistantMessage 的 null text 替换为空字符串。
     * 仅在检测到 null text 时才创建新对象，避免不必要的对象分配。
     */
    private ChatResponse sanitizeResponse(ChatResponse response) {
        if (response == null) {
            return response;
        }

        List<Generation> generations = response.getResults();
        if (generations == null || generations.isEmpty()) {
            return response;
        }

        boolean needsSanitization = false;
        for (Generation gen : generations) {
            if (gen.getOutput() != null && gen.getOutput().getText() == null) {
                needsSanitization = true;
                break;
            }
        }

        if (!needsSanitization) {
            return response;
        }

        List<Generation> newGenerations = generations.stream().map(gen -> {
            AssistantMessage output = gen.getOutput();
            if (output != null && output.getText() == null) {
                AssistantMessage sanitized = new AssistantMessage(
                        "", output.getMetadata(), output.getToolCalls(), output.getMedia());
                return new Generation(sanitized, gen.getMetadata());
            }
            return gen;
        }).toList();

        return new ChatResponse(newGenerations, response.getMetadata());
    }
}