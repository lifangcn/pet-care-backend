package pvt.mktech.petcare.entity;

import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * {@code @description}: 聊天消息文档（ES）
 * {@code @date}: 2026-03-02
 * @author Michael
 */
@Data
public class ChatMessageDocument {

    /**
     * 消息ID（雪花ID）
     */
    private Long id;

    /**
     * 对话ID（多轮会话共享）
     */
    private String conversationId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话ID（一次访问）
     */
    private String sessionId;

    /**
     * 会话名称（AI生成或用户自定义）
     */
    private String sessionName;

    /**
     * 角色：USER / ASSISTANT
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 向量（仅USER消息需要，1024维）
     */
    private List<Float> embedding;

    /**
     * 元数据（工具调用、token消耗等）
     */
    private Map<String, Object> metadata;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 过期时间（隐私保护）
     */
    private Instant expiresAt;
}
