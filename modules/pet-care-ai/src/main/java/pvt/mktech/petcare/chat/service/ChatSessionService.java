package pvt.mktech.petcare.chat.service;

import cn.hutool.core.util.IdUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.chat.dto.request.CreateSessionRequest;
import pvt.mktech.petcare.chat.dto.response.*;
import pvt.mktech.petcare.chat.repository.ChatHistoryRepository;
import pvt.mktech.petcare.entity.ChatMessageDocument;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@code @description}: 会话管理服务
 * {@code @date}: 2026-03-02
 * @author Michael
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatHistoryRepository chatHistoryRepository;
    private final ElasticsearchClient elasticsearchClient;

    @Value("${spring.ai.chat.memory.history.index-name:chat_history}")
    private String indexName;

    /**
     * 创建会话
     *
     * @param userId  用户ID
     * @param request 创建会话请求
     * @return 会话响应
     */
    public SessionResponse createSession(Long userId, CreateSessionRequest request) {
        String sessionId = IdUtil.simpleUUID();
        String name = (request.getName() != null && !request.getName().isBlank())
                ? request.getName()
                : "新对话";
        Instant now = Instant.now();

        return new SessionResponse(sessionId, name, now, now);
    }

    /**
     * 获取会话列表（聚合查询）
     *
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页大小
     * @return 会话列表响应
     */
    public SessionListResponse listSessions(Long userId, Long pageNumber, Long pageSize) {
        try {
            // 使用terms聚合获取所有session_id及其统计信息
            SearchResponse<ChatMessageDocument> response = elasticsearchClient.search(
                    s -> s.index(indexName)
                            .size(0) // 不返回文档，只返回聚合结果
                            .query(q -> q
                                    .term(t -> t
                                            .field("user_id")
                                            .value(userId))
                            )
                            .aggregations("sessions", agg -> agg
                                    .terms(t -> t
                                            .field("session_id")
                                            .size(1000) // 获取所有会话
                                    )
                                    .aggregations("first_message", subAgg -> subAgg
                                            .min(m -> m.field("created_at"))
                                    )
                                    .aggregations("last_message", subAgg -> subAgg
                                            .max(m -> m.field("created_at"))
                                    )
                            ),
                    ChatMessageDocument.class
            );

            // 解析聚合结果
            List<SessionItem> sessions = parseSessionAggregation(response.aggregations());

            // 分页
            int total = sessions.size();
            long from = pageNumber * pageSize;
            List<SessionItem> pagedSessions = sessions.stream()
                    .skip(from)
                    .limit(pageSize)
                    .toList();

            return new SessionListResponse((long) total, pagedSessions);

        } catch (Exception e) {
            log.error("获取会话列表失败: userId={}", userId, e);
            return new SessionListResponse(0L, new ArrayList<>());
        }
    }

    /**
     * 获取会话历史消息
     *
     * @param sessionId 会话ID
     * @param limit     最大返回条数
     * @return 消息列表
     */
    public List<ChatMessageResponse> getSessionMessages(String sessionId, int limit) {
        List<ChatMessageDocument> documents = chatHistoryRepository
                .getSessionHistory(sessionId, limit);

        return documents.stream()
                .map(this::toMessageResponse)
                .toList();
    }

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     */
    public void deleteSession(String sessionId) {
        long deletedCount = chatHistoryRepository.deleteBySessionId(sessionId);
        log.info("删除会话成功: sessionId={}, deletedCount={}", sessionId, deletedCount);
    }

    /**
     * 解析会话聚合结果
     */
    private List<SessionItem> parseSessionAggregation(Map<String, Aggregate> aggregations) {
        List<SessionItem> sessions = new ArrayList<>();

        // TODO: 完整解析ES聚合结果
        // 由于ES Java Client的聚合解析较复杂，这里返回空列表
        // 实际实现需要解析 StringTerms agg 和嵌套的 min/max agg

        log.warn("parseSessionAggregation 尚未完全实现，返回空列表");
        return sessions;
    }

    /**
     * 转换为消息响应
     */
    private ChatMessageResponse toMessageResponse(ChatMessageDocument doc) {
        // role 转小写，前端期望 user/assistant
        String role = doc.getRole() != null ? doc.getRole().toLowerCase() : "user";
        return new ChatMessageResponse(
                doc.getId(),
                role,
                doc.getContent(),
                doc.getCreatedAt()
        );
    }
}
