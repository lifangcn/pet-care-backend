package pvt.mktech.petcare.chat.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import pvt.mktech.petcare.entity.ChatMessageDocument;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * {@code @description}: 聊天历史记录ES操作封装
 * {@code @date}: 2026-03-02
 * @author Michael
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ChatHistoryRepository {

    private final ElasticsearchClient elasticsearchClient;
    private final EmbeddingModel embeddingModel;

    @Value("${spring.ai.chat.memory.history.index-name:chat_history}")
    private String indexName;

    /**
     * 批量保存消息
     *
     * @param messages 消息列表
     */
    public void batchSaveMessages(List<ChatMessageDocument> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        try {
            // 分离USER和ASSISTANT消息（仅USER消息需要向量）
            List<ChatMessageDocument> userMessages = messages.stream()
                    .filter(m -> "USER".equals(m.getRole()))
                    .toList();

            // 批量生成向量
            if (!userMessages.isEmpty()) {
                List<String> contents = userMessages.stream()
                        .map(ChatMessageDocument::getContent)
                        .toList();

                var embedResponse = embeddingModel.embedForResponse(contents);
                List<float[]> embeddings = embedResponse.getResults().stream()
                        .map(result -> result.getOutput())
                        .toList();

                // 填充向量到USER消息
                for (int i = 0; i < userMessages.size(); i++) {
                    float[] embedding = embeddings.get(i);
                    List<Float> vectorList = new ArrayList<>(embedding.length);
                    for (float v : embedding) {
                        vectorList.add(v);
                    }
                    userMessages.get(i).setEmbedding(vectorList);
                }
            }

            // 批量保存
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            for (ChatMessageDocument msg : messages) {
                final String id = String.valueOf(msg.getId());
                bulkBuilder.operations(op -> op
                        .index(idx -> idx
                                .index(indexName)
                                .id(id)
                                .document(msg))
                );
            }

            elasticsearchClient.bulk(bulkBuilder.build());
            log.info("批量保存聊天消息成功: count={}", messages.size());
        } catch (Exception e) {
            log.error("批量保存聊天消息失败: count={}", messages.size(), e);
        }
    }

    /**
     * 语义检索历史对话
     *
     * @param query   查询文本
     * @param userId  用户ID
     * @param topK    返回条数
     * @param minScore 最低相似度分数
     * @return 历史消息列表
     */
    public List<ChatMessageDocument> semanticSearch(String query, Long userId,
                                                    int topK, double minScore) {
        try {
            // 生成查询向量
            float[] queryVector = embeddingModel.embed(query);
            List<Float> vectorList = new ArrayList<>(queryVector.length);
            for (float v : queryVector) {
                vectorList.add(v);
            }

            // KNN检索 + 用户过滤
            SearchResponse<ChatMessageDocument> response = elasticsearchClient.search(
                    s -> s.index(indexName)
                            .size(topK)
                            .minScore(minScore)
                            .query(q -> q
                                    .bool(b -> b
                                            .must(knnQuery -> knnQuery
                                                    .knn(k -> k
                                                            .field("embedding")
                                                            .queryVector(vectorList)
                                                            .k(topK)
                                                            .numCandidates(topK * 2))
                                            )
                                            .filter(f -> f
                                                    .term(t -> t
                                                            .field("user_id")
                                                            .value(userId))
                                            )
                                    )
                            ),
                    ChatMessageDocument.class
            );

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.error("语义检索失败: query={}, userId={}", query, userId, e);
            return List.of();
        }
    }

    /**
     * 获取会话历史消息（按时间排序）
     *
     * @param sessionId 会话ID
     * @param limit     最大返回条数
     * @return 历史消息列表
     */
    public List<ChatMessageDocument> getSessionHistory(String sessionId, int limit) {
        try {
            SearchResponse<ChatMessageDocument> response = elasticsearchClient.search(
                    s -> s.index(indexName)
                            .size(limit)
                            .query(q -> q
                                    .term(t -> t
                                            .field("session_id")
                                            .value(sessionId))
                            )
                            .sort(sort -> sort
                                    .field(f -> f
                                            .field("created_at")
                                            .order(SortOrder.Asc))
                            ),
                    ChatMessageDocument.class
            );

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.error("获取会话历史失败: sessionId={}", sessionId, e);
            return List.of();
        }
    }

    /**
     * 删除用户所有历史记录
     *
     * @param userId 用户ID
     * @return 删除数量
     */
    public long deleteByUserId(Long userId) {
        try {
            DeleteByQueryResponse response = elasticsearchClient.deleteByQuery(
                    d -> d.index(indexName)
                            .query(q -> q
                                    .term(t -> t
                                            .field("user_id")
                                            .value(userId))
                            )
                            .refresh(true) // 立即刷新
            );

            log.info("删除用户历史记录: userId={}, deleted={}",
                    userId, response.deleted());
            return response.deleted();
        } catch (Exception e) {
            log.error("删除用户历史失败: userId={}", userId, e);
            throw new RuntimeException("删除历史记录失败", e);
        }
    }

    /**
     * 删除会话及其所有消息
     *
     * @param sessionId 会话ID
     * @return 删除数量
     */
    public long deleteBySessionId(String sessionId) {
        try {
            // 先查询会话的所有消息ID
            SearchResponse<ChatMessageDocument> searchResponse = elasticsearchClient.search(
                    s -> s.index(indexName)
                            .size(10000)
                            .query(q -> q
                                    .term(t -> t
                                            .field("session_id")
                                            .value(sessionId))
                            ),
                    ChatMessageDocument.class
            );

            // 批量删除
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            for (Hit<ChatMessageDocument> hit : searchResponse.hits().hits()) {
                String id = hit.id();
                bulkBuilder.operations(op -> op
                        .delete(idx -> idx
                                .index(indexName)
                                .id(id))
                );
            }

            if (!searchResponse.hits().hits().isEmpty()) {
                elasticsearchClient.bulk(bulkBuilder.build());
            }

            long count = searchResponse.hits().hits().size();
            log.info("删除会话消息: sessionId={}, count={}", sessionId, count);
            return count;
        } catch (Exception e) {
            log.error("删除会话消息失败: sessionId={}", sessionId, e);
            return 0;
        }
    }

    /**
     * 统计会话消息数
     *
     * @param sessionId 会话ID
     * @return 消息数量
     */
    public long countBySessionId(String sessionId) {
        try {
            CountResponse response = elasticsearchClient.count(
                    c -> c.index(indexName)
                            .query(q -> q
                                    .term(t -> t
                                            .field("session_id")
                                            .value(sessionId))
                            )
            );
            return response.count();
        } catch (Exception e) {
            log.error("统计会话消息数失败: sessionId={}", sessionId, e);
            return 0;
        }
    }

    /**
     * 聚合查询用户的所有会话（返回会话ID和最新消息时间）
     *
     * @param userId 用户ID
     * @param limit  返回条数
     * @return 会话列表：每项包含 sessionId, firstMessageTime, lastMessageTime, messageCount
     */
    public List<Map<String, Object>> aggregateUserSessions(Long userId, int limit) {
        try {
            // 使用聚合获取会话统计信息
            // TODO: 实现复杂的聚合查询，或使用terms agg + top_hits
            // 这里返回空列表，由Service层实现
            log.warn("aggregateUserSessions 尚未完全实现，返回空列表");
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("聚合用户会话失败: userId={}", userId, e);
            return new ArrayList<>();
        }
    }
}
