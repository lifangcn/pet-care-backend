package pvt.mktech.petcare.chat.sink;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.chat.event.ChatMessageSaveEvent;
import pvt.mktech.petcare.chat.repository.ChatHistoryRepository;
import pvt.mktech.petcare.chat.service.SessionTitleGenerator;

/**
 * {@code @description}: 聊天消息保存监听器
 * 异步保存消息到ES
 * {@code @date}: 2026-03-02
 * @author Michael
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageSaveListener {

    private final ChatHistoryRepository chatHistoryRepository;
    private final SessionTitleGenerator sessionTitleGenerator;

    /**
     * 监听保存事件，批量写入ES
     */
    @EventListener
    @Async("aiSyncThreadPoolExecutor")
    public void handleSaveEvent(ChatMessageSaveEvent event) {
        var documents = event == null ? null : event.getDocuments();
        int count = documents == null ? 0 : documents.size();
        Long owner = count == 0 || documents.getFirst() == null ? null : documents.getFirst().getUserId();
        String sessionId = count == 0 || documents.getFirst() == null ? null : effectiveSessionId(documents.getFirst().getSessionId());
        String conversationId = count == 0 || documents.getFirst() == null ? null : documents.getFirst().getConversationId();
        try {
            boolean eligibleForTitle = isSingleConversation(documents, owner, sessionId, conversationId);
            boolean firstSessionMessage = eligibleForTitle && chatHistoryRepository.countBySessionId(owner, sessionId) == 0;
            chatHistoryRepository.batchSaveMessages(documents);
            if (firstSessionMessage && "USER".equals(documents.getFirst().getRole())) {
                sessionTitleGenerator.generateTitle(owner, sessionId, documents.getFirst().getContent());
            }
            log.debug("批量保存聊天消息成功: owner={} sessionId={} conversationId={} count={}", owner, sessionId, conversationId, count);
        } catch (RuntimeException e) {
            log.error("批量保存聊天消息失败: owner={} sessionId={} conversationId={} count={}", owner, sessionId, conversationId, count, e);
            throw e;
        }
    }

    private boolean isSingleConversation(java.util.List<pvt.mktech.petcare.entity.ChatMessageDocument> documents, Long owner, String sessionId, String conversationId) {
        return documents != null && !documents.isEmpty() && owner != null && conversationId != null && !conversationId.isBlank()
                && documents.stream().allMatch(document -> document != null && owner.equals(document.getUserId())
                && sessionId.equals(effectiveSessionId(document.getSessionId())) && conversationId.equals(document.getConversationId()));
    }

    private String effectiveSessionId(String sessionId) { return sessionId == null || sessionId.isBlank() ? ChatHistoryRepository.DEFAULT_SESSION_ID : sessionId; }
}
