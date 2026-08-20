package pvt.mktech.petcare.chat.repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.zhipuai.ZhiPuAiEmbeddingModel;
import org.springframework.stereotype.Repository;
import pvt.mktech.petcare.chat.dto.response.SessionItem;
import pvt.mktech.petcare.chat.store.ChatHistoryStore;
import pvt.mktech.petcare.entity.ChatMessageDocument;
import pvt.mktech.petcare.infrastructure.config.ChatMemoryProperties;

/** The only history facade. Stores persist/query; this class owns embedding orchestration. */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ChatHistoryRepository {
    public static final String DEFAULT_SESSION_ID = "default";
    public static final String DEFAULT_SESSION_NAME = "新对话";
    private static final int MAX_SESSION_ID_LENGTH = 32;
    private static final int MAX_SESSION_NAME_LENGTH = 100;

    private final ChatHistoryStore store;
    private final ZhiPuAiEmbeddingModel embeddingModel;
    private final ChatMemoryProperties properties;

    public String normalizeSessionId(Long userId, String sessionId) {
        requireUserId(userId);
        if (sessionId == null || sessionId.isBlank()) {
            store.ensureSession(userId, DEFAULT_SESSION_ID, DEFAULT_SESSION_NAME);
            return DEFAULT_SESSION_ID;
        }
        validateSessionId(sessionId);
        if (store.getSession(userId, sessionId).isEmpty()) {
            throw new IllegalArgumentException("会话不存在或无权访问");
        }
        return sessionId;
    }

    public void createSession(Long userId, String sessionId, String name) {
        requireUserId(userId);
        validateSessionId(sessionId);
        if (name == null || name.isBlank() || name.length() > MAX_SESSION_NAME_LENGTH) {
            throw new IllegalArgumentException("会话名称不能为空且长度不能超过 100");
        }
        store.ensureSession(userId, sessionId, name);
    }

    public Optional<SessionItem> getSession(Long userId, String sessionId) {
        requireUserId(userId);
        validateSessionId(sessionId);
        return store.getSession(userId, sessionId);
    }

    public List<SessionItem> listSessions(Long userId, int offset, int limit) {
        requireUserId(userId);
        if (offset < 0 || limit < 1) {
            throw new IllegalArgumentException("分页参数无效");
        }
        return store.listSessions(userId, offset, limit);
    }

    public long countSessions(Long userId) { requireUserId(userId); return store.countSessions(userId); }

    public void batchSaveMessages(List<ChatMessageDocument> messages) {
        if (messages == null || messages.isEmpty()) return;

        BatchContext context = validateBatch(messages);
        if (context.defaultSession()) {
            store.ensureSession(context.userId(), DEFAULT_SESSION_ID, DEFAULT_SESSION_NAME);
        } else if (store.getSession(context.userId(), context.sessionId()).isEmpty()) {
            throw new IllegalArgumentException("会话不存在或无权访问");
        }

        Instant expiresAt = Instant.now().plus(properties.getHistory().getRetentionDays(), ChronoUnit.DAYS);
        for (ChatMessageDocument message : messages) {
            message.setSessionId(context.sessionId());
            message.setEmbedding(null);
            if (message.getExpiresAt() == null) message.setExpiresAt(expiresAt);
        }
        store.saveMessages(messages);

        List<ChatMessageDocument> users = messages.stream().filter(message -> "USER".equals(message.getRole())).toList();
        if (users.isEmpty()) return;
        try {
            List<float[]> output = embeddingModel.embedForResponse(users.stream().map(ChatMessageDocument::getContent).toList())
                    .getResults().stream().map(Embedding::getOutput).toList();
            if (output.size() != users.size()) throw new IllegalStateException("embedding 数量不匹配");
            for (int i = 0; i < users.size(); i++) users.get(i).setEmbedding(toVector(output.get(i)));
            store.updateEmbeddings(users);
        } catch (Exception e) {
            users.forEach(message -> message.setEmbedding(null));
            log.error("chat_embedding_degraded userId={} sessionId={} count={} reason=embedding_failed", context.userId(), context.sessionId(), users.size(), e);
        }
    }

    public List<ChatMessageDocument> semanticSearch(String query, Long userId, int topK, double minScore) {
        requireUserId(userId);
        if (query == null || query.isBlank()) throw new IllegalArgumentException("查询内容不能为空");
        if (topK < 1 || topK > 20) throw new IllegalArgumentException("topK 必须在 1 到 20 之间");
        if (!Double.isFinite(minScore) || minScore < 0 || minScore > 1) throw new IllegalArgumentException("minScore 必须在 0 到 1 之间");
        int historyDays = properties.getSemantic().getHistoryDays();
        if (historyDays < 1) throw new IllegalArgumentException("historyDays 必须大于等于 1");

        List<Float> vector;
        try {
            vector = toVector(embeddingModel.embed(query));
        } catch (Exception e) {
            log.error("chat_semantic_degraded userId={} reason=embedding_failed", userId, e);
            return List.of();
        }
        try {
            return store.semanticSearch(userId, vector, topK, minScore, historyDays);
        } catch (Exception e) {
            log.error("chat_semantic_degraded userId={} reason=store_query_failed", userId, e);
            return List.of();
        }
    }

    public List<ChatMessageDocument> getSessionHistory(Long userId, String sessionId, int limit) { return store.getSessionHistory(userId, sessionId, limit); }
    public long deleteByUserId(Long userId) { return store.deleteByUserId(userId); }
    public long deleteBySessionId(Long userId, String sessionId) { return store.deleteSession(userId, sessionId); }
    public long countBySessionId(Long userId, String sessionId) { return store.countMessages(userId, sessionId); }
    public boolean updateSessionName(Long userId, String sessionId, String name) { return store.updateDefaultSessionName(userId, sessionId, name); }
    public int deleteExpiredBatch(int size) { return store.deleteExpiredBatch(size); }

    private BatchContext validateBatch(List<ChatMessageDocument> messages) {
        ChatMessageDocument first = messages.getFirst();
        if (first == null) throw new IllegalArgumentException("消息不能为空");
        Long userId = first.getUserId();
        requireUserId(userId);
        String sessionId = effectiveSessionId(first.getSessionId());
        String conversationId = first.getConversationId();
        validateMessage(first);
        for (ChatMessageDocument message : messages) {
            if (message == null) throw new IllegalArgumentException("消息不能为空");
            validateMessage(message);
            if (!userId.equals(message.getUserId())) throw new IllegalArgumentException("消息 owner 必须一致");
            if (!sessionId.equals(effectiveSessionId(message.getSessionId()))) throw new IllegalArgumentException("消息 session 必须一致");
            if (!conversationId.equals(message.getConversationId())) throw new IllegalArgumentException("消息 conversation 必须一致");
        }
        return new BatchContext(userId, sessionId, DEFAULT_SESSION_ID.equals(sessionId));
    }

    private void validateMessage(ChatMessageDocument message) {
        requireUserId(message.getUserId());
        validateSessionIdIfPresent(message.getSessionId());
        if (message.getId() == null || message.getCreatedAt() == null || message.getContent() == null || message.getConversationId() == null || message.getConversationId().isBlank()) {
            throw new IllegalArgumentException("消息 id、createdAt、content 和 conversationId 不能为空");
        }
        if (!"USER".equals(message.getRole()) && !"ASSISTANT".equals(message.getRole())) throw new IllegalArgumentException("消息 role 必须为 USER 或 ASSISTANT");
    }

    private String effectiveSessionId(String sessionId) { return sessionId == null || sessionId.isBlank() ? DEFAULT_SESSION_ID : sessionId; }
    private void validateSessionIdIfPresent(String sessionId) { if (sessionId != null && !sessionId.isBlank()) validateSessionId(sessionId); }
    private void validateSessionId(String sessionId) { if (sessionId == null || sessionId.isBlank() || sessionId.length() > MAX_SESSION_ID_LENGTH) throw new IllegalArgumentException("sessionId 不能为空且长度不能超过 32"); }
    private void requireUserId(Long userId) { if (userId == null) throw new IllegalArgumentException("userId 不能为空"); }
    private List<Float> toVector(float[] values) { if (values == null || values.length != 1024) throw new IllegalArgumentException("embedding 必须为 1024 维"); List<Float> result = new ArrayList<>(1024); for (float value : values) { if (!Float.isFinite(value)) throw new IllegalArgumentException("embedding 必须为有限数"); result.add(value); } return result; }

    private record BatchContext(Long userId, String sessionId, boolean defaultSession) { }
}
