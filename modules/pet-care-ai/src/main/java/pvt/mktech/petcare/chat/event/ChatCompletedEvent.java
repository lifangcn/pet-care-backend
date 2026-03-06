package pvt.mktech.petcare.chat.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;

/**
 * {@code @description}: 对话完成事件
 * {@code @date}: 2026-03-02
 * @author Michael
 */
@Getter
public class ChatCompletedEvent extends ApplicationEvent {

    /**
     * 用户ID
     */
    private final Long userId;

    /**
     * 会话ID
     */
    private final String sessionId;

    /**
     * 对话ID
     */
    private final String conversationId;

    /**
     * 用户消息
     */
    private final String userMessage;

    /**
     * AI完整响应
     */
    private final String aiResponse;

    /**
     * 事件时间戳
     */
    private final Instant eventTimestamp;

    public ChatCompletedEvent(Long userId, String sessionId, String conversationId,
                              String userMessage, String aiResponse) {
        super(userId);
        this.userId = userId;
        this.sessionId = sessionId;
        this.conversationId = conversationId;
        this.userMessage = userMessage;
        this.aiResponse = aiResponse;
        this.eventTimestamp = Instant.now();
    }
}
