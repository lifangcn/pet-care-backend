package pvt.mktech.petcare.chat.store;

import pvt.mktech.petcare.chat.dto.response.SessionItem;
import pvt.mktech.petcare.entity.ChatMessageDocument;

import java.util.List;
import java.util.Optional;

/** Physical persistence boundary for chat history.  It deliberately does not embed text. */
public interface ChatHistoryStore {
    void ensureSession(Long userId, String sessionId, String name);
    Optional<SessionItem> getSession(Long userId, String sessionId);
    List<SessionItem> listSessions(Long userId, int offset, int limit);
    long countSessions(Long userId);
    void saveMessages(List<ChatMessageDocument> messages);
    void updateEmbeddings(List<ChatMessageDocument> messages);
    List<ChatMessageDocument> getSessionHistory(Long userId, String sessionId, int limit);
    List<ChatMessageDocument> semanticSearch(Long userId, List<Float> vector, int topK, double minScore, int historyDays);
    long deleteSession(Long userId, String sessionId);
    long deleteByUserId(Long userId);
    long countMessages(Long userId, String sessionId);
    boolean updateDefaultSessionName(Long userId, String sessionId, String name);
    int deleteExpiredBatch(int batchSize);
}
