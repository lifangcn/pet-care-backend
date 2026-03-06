package pvt.mktech.petcare.chat.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import pvt.mktech.petcare.entity.ChatMessageDocument;

import java.util.List;

/**
 * {@code @description}: 消息保存事件
 * {@code @date}: 2026-03-02
 * @author Michael
 */
@Getter
public class ChatMessageSaveEvent extends ApplicationEvent {

    /**
     * 待保存的消息列表
     */
    private final List<ChatMessageDocument> documents;

    public ChatMessageSaveEvent(List<ChatMessageDocument> documents) {
        super(documents);
        this.documents = documents;
    }
}
