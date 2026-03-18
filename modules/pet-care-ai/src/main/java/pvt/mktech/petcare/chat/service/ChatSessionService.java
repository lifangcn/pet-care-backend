package pvt.mktech.petcare.chat.service;

import cn.hutool.core.util.IdUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.MaxAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.MinAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.chat.dto.request.CreateSessionRequest;
import pvt.mktech.petcare.chat.dto.response.ChatMessageResponse;
import pvt.mktech.petcare.chat.dto.response.SessionItem;
import pvt.mktech.petcare.chat.dto.response.SessionListResponse;
import pvt.mktech.petcare.chat.dto.response.SessionResponse;
import pvt.mktech.petcare.chat.repository.ChatHistoryRepository;
import pvt.mktech.petcare.entity.ChatMessageDocument;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@code @description}: 会话管理服务
 * {@code @date}: 2026-03-02
 *
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
     * @param userId     用户ID
     * @param pageNumber 页码
     * @param pageSize   每页大小
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
                                    .aggregations("session_name", subAgg -> subAgg
                                            .topHits(th -> th
                                                    .size(1)
                                                    .sort(sort -> sort
                                                            .field(f -> f
                                                                    .field("created_at")
                                                                    .order(SortOrder.Asc)
                                                            )
                                                    )
                                            )
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
     * @param userId   用户ID（验证权限）
     * @param sessionId 会话ID
     * @param limit     最大返回条数
     * @return 消息列表
     */
    public List<ChatMessageResponse> getSessionMessages(Long userId, String sessionId, int limit) {
        List<ChatMessageDocument> documents = chatHistoryRepository
                .getSessionHistory(sessionId, limit);

        // 权限验证：确保会话属于当前用户
        if (!documents.isEmpty()) {
            Long docUserId = documents.get(0).getUserId();
            if (!docUserId.equals(userId)) {
                throw new IllegalArgumentException("无权访问该会话");
            }
        }

        return documents.stream()
                .map(this::toMessageResponse)
                .toList();
    }

    /**
     * 删除会话
     *
     * @param userId   用户ID（验证权限）
     * @param sessionId 会话ID
     */
    public void deleteSession(Long userId, String sessionId) {
        // 权限验证：检查会话是否属于当前用户
        List<ChatMessageDocument> documents = chatHistoryRepository
                .getSessionHistory(sessionId, 1);
        if (!documents.isEmpty()) {
            Long docUserId = documents.get(0).getUserId();
            if (!docUserId.equals(userId)) {
                throw new IllegalArgumentException("无权删除该会话");
            }
        }

        long deletedCount = chatHistoryRepository.deleteBySessionId(sessionId);
        log.info("删除会话成功: sessionId={}, deletedCount={}", sessionId, deletedCount);
    }

    /**
     * 解析会话聚合结果
     * {@code @date}: 2026-03-09
     * @author Michael
     */
    private List<SessionItem> parseSessionAggregation(Map<String, Aggregate> aggregations) {
        List<SessionItem> sessions = new ArrayList<>();

        Aggregate sessionsAgg = aggregations.get("sessions");
        if (sessionsAgg == null || !sessionsAgg.isSterms()) {
            return sessions;
        }

        StringTermsAggregate termsAgg = sessionsAgg.sterms();
        if (termsAgg == null || termsAgg.buckets() == null) {
            return sessions;
        }

        for (var bucket : termsAgg.buckets().array()) {
            String sessionId = bucket.key().stringValue();

            // 解析子聚合
            MinAggregate firstMsgAgg = bucket.aggregations().get("first_message") != null
                    ? bucket.aggregations().get("first_message").min()
                    : null;
            MaxAggregate lastMsgAgg = bucket.aggregations().get("last_message") != null
                    ? bucket.aggregations().get("last_message").max()
                    : null;
            Aggregate nameAgg = bucket.aggregations().get("session_name");
            Long docCount = bucket.docCount();

            Instant createdAt = null;
            if (firstMsgAgg != null && !Double.isNaN(firstMsgAgg.value())) {
                createdAt = Instant.ofEpochMilli((long) firstMsgAgg.value());
            }

            Instant updatedAt = null;
            if (lastMsgAgg != null && !Double.isNaN(lastMsgAgg.value())) {
                updatedAt = Instant.ofEpochMilli((long) lastMsgAgg.value());
            }

            // 从 top_hits 中获取 session_name
            String sessionName = sessionId; // 默认值
            if (nameAgg != null && nameAgg.isTopHits()) {
                var topHits = nameAgg.topHits();
                if (topHits != null && topHits.hits() != null && !topHits.hits().hits().isEmpty()) {
                    var hit = topHits.hits().hits().get(0);
                    if (hit.source() != null) {
                        var doc = hit.source().to(ChatMessageDocument.class);
                        if (doc != null && doc.getSessionName() != null) {
                            sessionName = doc.getSessionName();
                        }
                    }
                }
            }

            sessions.add(new SessionItem(sessionId, sessionName, createdAt, updatedAt, docCount));
        }

        // 按 updatedAt 降序排序
        sessions.sort((a, b) -> {
            if (a.getUpdatedAt() == null) return 1;
            if (b.getUpdatedAt() == null) return -1;
            return b.getUpdatedAt().compareTo(a.getUpdatedAt());
        });

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
