package pvt.mktech.petcare.chat.sink;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.chat.event.ChatCompletedEvent;
import pvt.mktech.petcare.chat.event.ChatMessageSaveEvent;
import pvt.mktech.petcare.entity.ChatMessageDocument;
import pvt.mktech.petcare.infrastructure.config.ChatMemoryProperties;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code @description}: 聊天消息异步存储处理器
 * 监听对话完成事件，异步保存消息到ES
 * {@code @date}: 2026-03-02
 * @author Michael
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageSink {

    private final ApplicationEventPublisher eventPublisher;
    private final ChatMemoryProperties properties;

    /**
     * 处理对话完成（流式响应）
     * 在Flux.doOnComplete()中调用
     *
     * @param userId         用户ID
     * @param sessionId      会话ID
     * @param conversationId 对话ID
     * @param userMessage    用户消息
     * @param aiResponse     AI完整响应
     */
    public void onChatCompleted(Long userId, String sessionId, String conversationId,
                                String userMessage, String aiResponse) {

        if (!properties.getHistory().isEnabled()) {
            return;
        }

        // 发布事件，异步处理
        ChatCompletedEvent event = new ChatCompletedEvent(
                userId, sessionId, conversationId, userMessage, aiResponse);
        eventPublisher.publishEvent(event);
    }

    /**
     * 事件监听器 - 异步保存消息
     */
    @EventListener
    @Async("aiSyncThreadPoolExecutor")
    public void handleChatCompletedEvent(ChatCompletedEvent event) {
        try {
            List<ChatMessageDocument> documents = new ArrayList<>();

            // 用户消息
            ChatMessageDocument userMsg = createMessageDocument(
                    event.getUserId(),
                    event.getSessionId(),
                    event.getConversationId(),
                    "USER",
                    event.getUserMessage()
            );
            documents.add(userMsg);

            // AI响应
            ChatMessageDocument aiMsg = createMessageDocument(
                    event.getUserId(),
                    event.getSessionId(),
                    event.getConversationId(),
                    "ASSISTANT",
                    event.getAiResponse()
            );
            documents.add(aiMsg);

            // 发布保存事件
            ChatMessageSaveEvent saveEvent = new ChatMessageSaveEvent(documents);
            eventPublisher.publishEvent(saveEvent);

        } catch (Exception e) {
            log.error("处理聊天完成事件失败: conversationId={}",
                    event.getConversationId(), e);
        }
    }

    /**
     * 监听保存事件，委托给Repository批量写入ES
     * 注意：实际的ES写入在Repository中完成，这里可以做额外的处理
     */
    @EventListener
    @Async("aiSyncThreadPoolExecutor")
    public void handleSaveEvent(ChatMessageSaveEvent event) {
        // 实际的ES写入由ChatMessageSaveEvent的另一监听器处理
        // 这里可以添加额外的处理逻辑，如统计、监控等
        log.debug("收到消息保存事件: documentCount={}", event.getDocuments().size());
    }

    /**
     * 创建消息文档
     */
    private ChatMessageDocument createMessageDocument(Long userId, String sessionId,
                                                      String conversationId, String role,
                                                      String content) {
        ChatMessageDocument doc = new ChatMessageDocument();
        doc.setId(IdUtil.getSnowflakeNextId());
        doc.setUserId(userId);
        doc.setSessionId(sessionId);
        doc.setConversationId(conversationId);
        doc.setRole(role);
        doc.setContent(content);
        doc.setCreatedAt(Instant.now());

        // 设置过期时间
        Instant expiresAt = Instant.now().plus(
                properties.getHistory().getRetentionDays(), ChronoUnit.DAYS);
        doc.setExpiresAt(expiresAt);

        return doc;
    }
}
