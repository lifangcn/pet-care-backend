package pvt.mktech.petcare.chat.sink;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.chat.event.ChatMessageSaveEvent;
import pvt.mktech.petcare.chat.repository.ChatHistoryRepository;

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

    /**
     * 监听保存事件，批量写入ES
     */
    @EventListener
    @Async("aiSyncThreadPoolExecutor")
    public void handleSaveEvent(ChatMessageSaveEvent event) {
        try {
            chatHistoryRepository.batchSaveMessages(event.getDocuments());
            log.debug("批量保存聊天消息成功: count={}", event.getDocuments().size());
        } catch (Exception e) {
            log.error("保存聊天消息失败: count={}", event.getDocuments().size(), e);
        }
    }
}
