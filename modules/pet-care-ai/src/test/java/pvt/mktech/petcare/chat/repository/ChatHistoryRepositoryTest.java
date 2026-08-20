package pvt.mktech.petcare.chat.repository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.zhipuai.ZhiPuAiEmbeddingModel;
import pvt.mktech.petcare.chat.store.ChatHistoryStore;
import pvt.mktech.petcare.entity.ChatMessageDocument;
import pvt.mktech.petcare.infrastructure.config.ChatMemoryProperties;

@ExtendWith(MockitoExtension.class)
class ChatHistoryRepositoryTest {
    @Mock private ChatHistoryStore store;
    @Mock private ZhiPuAiEmbeddingModel embeddingModel;
    private ChatMemoryProperties properties;
    private ChatHistoryRepository repository;

    @BeforeEach
    void setUp() { properties = new ChatMemoryProperties(); repository = new ChatHistoryRepository(store, embeddingModel, properties); }

    @Test
    void defaultSessionIsEnsuredButExplicitSessionMustExist() {
        assertEquals("default", repository.normalizeSessionId(1L, " "));
        verify(store).ensureSession(1L, "default", "新对话");

        assertThrows(IllegalArgumentException.class, () -> repository.normalizeSessionId(1L, "missing"));
        verify(store).getSession(1L, "missing");
    }

    @Test
    void invalidBatchIsRejectedBeforeAnyStoreOperation() {
        ChatMessageDocument invalid = message(1L, null, "conversation", "SYSTEM");
        assertThrows(IllegalArgumentException.class, () -> repository.batchSaveMessages(List.of(invalid)));
        verifyNoInteractions(store);
    }

    @Test
    void mixedOwnerSessionAndConversationAreRejectedBeforeWrites() {
        assertInvalidBatch(message(1L, "s1", "c1", "USER"), message(2L, "s1", "c1", "ASSISTANT"));
        assertInvalidBatch(message(1L, "s1", "c1", "USER"), message(1L, "s2", "c1", "ASSISTANT"));
        assertInvalidBatch(message(1L, "s1", "c1", "USER"), message(1L, "s1", "c2", "ASSISTANT"));
    }

    @Test
    void primaryWriteFailureIsPropagated() {
        when(store.getSession(1L, "s1")).thenReturn(Optional.of(mock(pvt.mktech.petcare.chat.dto.response.SessionItem.class)));
        org.mockito.Mockito.doThrow(new IllegalStateException("store unavailable")).when(store).saveMessages(anyList());
        ChatMessageDocument document = message(1L, "s1", "c1", "ASSISTANT");
        assertThrows(IllegalStateException.class, () -> repository.batchSaveMessages(List.of(document)));
        verify(store).saveMessages(anyList());
    }

    @Test
    void embeddingFailureKeepsSavedUserMessageWithoutEmbedding() {
        when(store.getSession(1L, "s1")).thenReturn(Optional.of(mock(pvt.mktech.petcare.chat.dto.response.SessionItem.class)));
        when(embeddingModel.embedForResponse(anyList())).thenThrow(new IllegalStateException("embedding unavailable"));
        ChatMessageDocument document = message(1L, "s1", "c1", "USER");

        assertDoesNotThrow(() -> repository.batchSaveMessages(List.of(document)));

        assertEquals(null, document.getEmbedding());
        verify(store).saveMessages(anyList());
        verify(store, never()).updateEmbeddings(anyList());
    }

    @Test
    void semanticInputBoundsAreValidatedAndEmbeddingFailureDegradesToEmpty() {
        assertThrows(IllegalArgumentException.class, () -> repository.semanticSearch("q", 1L, 0, .5));
        assertThrows(IllegalArgumentException.class, () -> repository.semanticSearch("q", 1L, 21, .5));
        assertThrows(IllegalArgumentException.class, () -> repository.semanticSearch("q", 1L, 1, 1.1));
        properties.getSemantic().setHistoryDays(0);
        assertThrows(IllegalArgumentException.class, () -> repository.semanticSearch("q", 1L, 1, .5));
        properties.getSemantic().setHistoryDays(1);
        when(embeddingModel.embed("q")).thenThrow(new IllegalStateException("unavailable"));
        assertEquals(List.of(), repository.semanticSearch("q", 1L, 1, .5));
        verify(store, never()).semanticSearch(any(), anyList(), anyInt(), anyDouble(), anyInt());
    }

    private void assertInvalidBatch(ChatMessageDocument first, ChatMessageDocument second) {
        assertThrows(IllegalArgumentException.class, () -> repository.batchSaveMessages(List.of(first, second)));
        verifyNoInteractions(store);
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
