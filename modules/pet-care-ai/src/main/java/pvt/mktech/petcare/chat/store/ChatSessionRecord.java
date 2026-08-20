package pvt.mktech.petcare.chat.store;

import java.time.Instant;

public record ChatSessionRecord(String sessionId, Long userId, String name, Instant createdAt, Instant updatedAt,
                                Instant expiresAt, long messageCount) { }
