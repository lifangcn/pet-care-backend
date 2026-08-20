package pvt.mktech.petcare.chat.sink;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pvt.mktech.petcare.chat.event.ChatMessageSaveEvent;
import pvt.mktech.petcare.chat.repository.ChatHistoryRepository;
import pvt.mktech.petcare.chat.service.SessionTitleGenerator;
import pvt.mktech.petcare.entity.ChatMessageDocument;

@ExtendWith(MockitoExtension.class)
class ChatMessageSaveListenerTest {
    @Mock private ChatHistoryRepository history;
    @Mock private SessionTitleGenerator titleGenerator;
    private ChatMessageSaveListener listener;

    @BeforeEach
    void setUp() { listener = new ChatMessageSaveListener(history, titleGenerator); }

    @Test
    void firstUserMessageTriggersTitleOnlyOnce() {
        ChatMessageDocument user = message(1L, "s1", "c1", "USER");
        when(history.countBySessionId(1L, "s1")).thenReturn(0L);

        listener.handleSaveEvent(new ChatMessageSaveEvent(List.of(user)));

        verify(titleGenerator).generateTitle(1L, "s1", "content");
    }

    @Test
    void mixedBatchNeverTriggersTitle() {
        listener.handleSaveEvent(new ChatMessageSaveEvent(List.of(
                message(1L, "s1", "c1", "USER"), message(2L, "s1", "c1", "ASSISTANT"))));
        verify(titleGenerator, never()).generateTitle(any(), any(), any());
    }

    @Test
    void saveFailureIsRethrownAndDoesNotReportTitle() {
        ChatMessageDocument user = message(1L, "s1", "c1", "USER");
        when(history.countBySessionId(1L, "s1")).thenReturn(0L);
        doThrow(new IllegalStateException("store failed")).when(history).batchSaveMessages(any());

        assertThrows(IllegalStateException.class, () -> listener.handleSaveEvent(new ChatMessageSaveEvent(List.of(user))));
        verify(titleGenerator, never()).generateTitle(any(), any(), any());
    }

    private ChatMessageDocument message(Long owner, String sessionId, String conversationId, String role) {
        ChatMessageDocument document = new ChatMessageDocument();
        document.setId(1L);
        document.setUserId(owner);
        document.setSessionId(sessionId);
        document.setConversationId(conversationId);
        document.setRole(role);
        document.setContent("content");
        document.setCreatedAt(Instant.now());
        return document;
    }
}
