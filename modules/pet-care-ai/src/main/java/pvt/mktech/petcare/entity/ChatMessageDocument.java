package pvt.mktech.petcare.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("document_type")
    private String documentType = "message";

    /**
     * 消息ID（雪花ID）
     */
    @JsonProperty("id")
    private Long id;

    /**
     * 对话ID（多轮会话共享）
     */
    @JsonProperty("conversation_id")
    private String conversationId;

    /**
     * 用户ID
     */
    @JsonProperty("user_id")
    private Long userId;

    /**
     * 会话ID（一次访问）
     */
    @JsonProperty("session_id")
    private String sessionId;

    /**
     * 会话名称（AI生成或用户自定义）
     */
    @JsonProperty("session_name")
    private String sessionName;

    /**
     * 角色：USER / ASSISTANT
     */
    @JsonProperty("role")
    private String role;

    /**
     * 消息内容
     */
    @JsonProperty("content")
    private String content;

    /**
     * 向量（仅USER消息需要，1024维）
     */
    @JsonProperty("embedding")
    private List<Float> embedding;

    /**
     * 元数据（工具调用、token消耗等）
     */
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    /**
     * 创建时间
     */
    @JsonProperty("created_at")
    private Instant createdAt;

    /**
     * 过期时间（隐私保护）
     */
    @JsonProperty("expires_at")
    private Instant expiresAt;
}
