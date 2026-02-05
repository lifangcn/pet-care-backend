package pvt.mktech.petcare.shared;

import org.springframework.stereotype.Component;

/**
 * {@code @description}: 会话ID生成器
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Component
public class ConversationIdGenerator {

    private static final String SEPARATOR = ":";

    /**
     * 生成会话ID
     * 格式：userId:sessionId
     *
     * @param userId    用户ID
     * @param sessionId 会话ID（可选，如果为null则使用默认会话）
     * @return 会话ID
     */
    public String generate(Long userId, String sessionId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        if (sessionId == null || sessionId.isEmpty()) {
            // 默认会话：userId:default
            return userId + SEPARATOR + "default";
        }

        return userId + SEPARATOR + sessionId;
    }

    /**
     * 生成默认会话ID（同一用户只有一个会话）
     */
    public String generateDefault(Long userId) {
        return generate(userId, null);
    }

    /**
     * 从会话ID中提取用户ID
     */
    public Long extractUserId(String conversationId) {
        if (conversationId == null || !conversationId.contains(SEPARATOR)) {
            return null;
        }
        try {
            String userIdStr = conversationId.split(SEPARATOR)[0];
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
