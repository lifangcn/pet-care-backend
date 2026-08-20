package pvt.mktech.petcare.chat.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.annotation.UseDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pvt.mktech.petcare.chat.dto.response.SessionItem;
import pvt.mktech.petcare.chat.mapper.ChatHistoryMapper;
import pvt.mktech.petcare.entity.ChatMessageDocument;
import pvt.mktech.petcare.infrastructure.config.ChatMemoryProperties;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@UseDataSource("ai")
@ConditionalOnProperty(prefix = "spring.ai.chat.memory.history", name = "store", havingValue = "postgresql")
public class PostgresqlChatHistoryStore implements ChatHistoryStore {
    private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() { };

    private final ChatHistoryMapper mapper;
    private final ChatMemoryProperties properties;
    private final ObjectMapper objectMapper;

    public void ensureSession(Long userId, String id, String name) {
        mapper.ensureSession(userId, id, name, properties.getHistory().getRetentionDays());
    }

    public Optional<SessionItem> getSession(Long userId, String id) {
        return Optional.ofNullable(mapper.getSession(userId, id)).map(this::item);
    }

    public List<SessionItem> listSessions(Long userId, int offset, int limit) {
        return mapper.listSessions(userId, offset, limit).stream().map(this::item).toList();
    }

    public long countSessions(Long userId) { return mapper.countSessions(userId); }

    @Transactional
    public void saveMessages(List<ChatMessageDocument> messages) {
        if (messages == null || messages.isEmpty()) return;
        List<PostgresqlChatMessagePayload> payloads = messages.stream().map(this::payload).toList();
        Long userId = payloads.getFirst().userId();
        String sessionId = payloads.getFirst().sessionId();
        if (payloads.stream().anyMatch(payload -> !userId.equals(payload.userId()) || !sessionId.equals(payload.sessionId()))) {
            throw new IllegalArgumentException("A message batch must belong to one user session");
        }
        Instant maxCreatedAt = null;
        Instant maxExpiresAt = null;
        for (PostgresqlChatMessagePayload payload : payloads) {
            if (mapper.insertMessage(payload) == 1) {
                maxCreatedAt = max(maxCreatedAt, payload.createdAt());
                maxExpiresAt = max(maxExpiresAt, payload.expiresAt());
            }
        }
        if (maxCreatedAt != null) mapper.touch(userId, sessionId, maxCreatedAt, maxExpiresAt);
    }

    @Transactional
    public void updateEmbeddings(List<ChatMessageDocument> messages) {
        if (messages == null || messages.isEmpty()) return;
        List<PostgresqlChatMessagePayload> payloads = messages.stream().map(this::payload).toList();
        for (PostgresqlChatMessagePayload payload : payloads) {
            if (payload.embedding() != null) mapper.updateEmbedding(payload.id(), payload.userId(), payload.sessionId(), payload.embedding());
        }
    }

    public List<ChatMessageDocument> getSessionHistory(Long userId, String id, int limit) {
        return mapper.history(userId, id, limit).stream().map(this::document).toList();
    }

    public List<ChatMessageDocument> semanticSearch(Long userId, List<Float> vector, int topK, double minScore, int historyDays) {
        requireSemanticArguments(userId, vector, topK, minScore, historyDays);
        int candidateLimit = Math.max(40, Math.multiplyExact(topK, 10));
        return mapper.semantic(userId, vector(vector), topK, candidateLimit, minScore, historyDays).stream().map(this::document).toList();
    }

    @Transactional
    public long deleteSession(Long userId, String id) {
        long messageCount = mapper.countMessagesForDeletion(userId, id);
        mapper.deleteSession(userId, id);
        return messageCount;
    }

    @Transactional
    public long deleteByUserId(Long userId) {
        long messageCount = mapper.countMessagesForUserDeletion(userId);
        mapper.deleteUser(userId);
        return messageCount;
    }

    public long countMessages(Long userId, String id) { return mapper.countMessages(userId, id); }

    public boolean updateDefaultSessionName(Long userId, String id, String name) {
        return mapper.updateDefaultName(userId, id, name) > 0;
    }

    @Transactional
    public int deleteExpiredBatch(int size) {
        if (size <= 0) throw new IllegalArgumentException("batch size must be greater than zero");
        return mapper.deleteExpiredMessages(size) + mapper.deleteEmptyExpiredSessions(size);
    }

    private PostgresqlChatMessagePayload payload(ChatMessageDocument message) {
        Objects.requireNonNull(message, "message must not be null");
        if (message.getId() == null || message.getUserId() == null || message.getConversationId() == null || message.getSessionId() == null
                || message.getRole() == null || message.getContent() == null || message.getCreatedAt() == null || message.getExpiresAt() == null
                || message.getExpiresAt().isBefore(message.getCreatedAt())) {
            throw new IllegalArgumentException("message has required fields missing or invalid timestamps");
        }
        if (!"USER".equals(message.getRole()) && !"ASSISTANT".equals(message.getRole())) {
            throw new IllegalArgumentException("message role must be USER or ASSISTANT");
        }
        return new PostgresqlChatMessagePayload(message.getId(), message.getConversationId(), message.getUserId(), message.getSessionId(),
                message.getRole(), message.getContent(), vector(message.getEmbedding()), metadata(message.getMetadata()), message.getCreatedAt(), message.getExpiresAt());
    }

    private ChatMessageDocument document(PostgresqlChatMessageRow row) {
        ChatMessageDocument document = new ChatMessageDocument();
        document.setId(row.id()); document.setConversationId(row.conversationId()); document.setUserId(row.userId());
        document.setSessionId(row.sessionId()); document.setRole(row.role()); document.setContent(row.content());
        document.setCreatedAt(row.createdAt()); document.setExpiresAt(row.expiresAt());
        try {
            document.setMetadata(objectMapper.readValue(row.metadataJson(), METADATA_TYPE));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize PostgreSQL chat metadata", exception);
        }
        return document;
    }

    private String metadata(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to serialize chat metadata", exception);
        }
    }

    private void requireSemanticArguments(Long userId, List<Float> vector, int topK, double minScore, int historyDays) {
        if (userId == null || topK <= 0 || historyDays <= 0 || !Double.isFinite(minScore) || minScore < 0 || minScore > 1) {
            throw new IllegalArgumentException("invalid semantic search arguments");
        }
        vector(vector);
    }

    private String vector(List<Float> vector) {
        if (vector == null) return null;
        if (vector.size() != 1024 || vector.stream().anyMatch(value -> value == null || !Float.isFinite(value))) {
            throw new IllegalArgumentException("embedding must contain 1024 finite values");
        }
        return "[" + vector.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")) + "]";
    }

    private Instant max(Instant left, Instant right) { return left == null || right.isAfter(left) ? right : left; }

    private SessionItem item(ChatSessionRecord record) {
        return new SessionItem(record.sessionId(), record.name(), record.createdAt(), record.updatedAt(), record.messageCount());
    }
}
