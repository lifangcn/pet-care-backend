package pvt.mktech.petcare.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.chat.repository.ChatHistoryRepository;

/**
 * {@code @description}: 会话标题生成服务，首条用户消息后异步生成简短标题
 * {@code @date}: 2026-03-09
 * @author Michael
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionTitleGenerator {

    private final ChatClient chatClient;
    private final ChatHistoryRepository chatHistoryRepository;

    /**
     * 异步生成会话标题
     *
     * @param userId          用户ID
     * @param sessionId       会话ID
     * @param firstUserMessage 首条用户消息
     */
    @Async
    public void generateTitle(Long userId, String sessionId, String firstUserMessage) {
        String prompt = "将以下用户输入概括为一个简洁的对话标题（不超过10个汉字），直接输出标题，不要任何解释：\n"
                + firstUserMessage;

        try {
            String title = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            String trimmedTitle = title != null ? title.trim() : null;
            if (trimmedTitle != null && !trimmedTitle.isEmpty()) {
                chatHistoryRepository.updateSessionName(sessionId, trimmedTitle);
                log.info("会话标题生成成功: sessionId={}, title={}", sessionId, trimmedTitle);
            }
        } catch (Exception e) {
            log.error("会话标题生成失败: sessionId={}", sessionId, e);
        }
    }
}
