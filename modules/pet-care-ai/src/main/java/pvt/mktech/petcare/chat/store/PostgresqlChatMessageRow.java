package pvt.mktech.petcare.chat.store;

import java.time.Instant;

public record PostgresqlChatMessageRow(Long id, String conversationId, Long userId, String sessionId, String role,
                                        String content, String metadataJson, Instant createdAt, Instant expiresAt) { }
