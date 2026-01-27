package pvt.mktech.petcare.cdc.service;

import cn.hutool.core.lang.TypeReference;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@code @description}: Post 批量向量化服务
 * <p>定时扫描 ES 中未向量的文档，批量调用 DashScope 生成向量</p>
 * {@code @date}: 2026-01-27
 *
 * @author Michael
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostVectorizationService {

    private final ElasticsearchClient elasticsearchClient;
    private final EmbeddingModel embeddingModel;

    private static final String POST_INDEX = "post";
    private static final int BATCH_SIZE = 10;  // 每批处理 10 条

    /**
     * 批量化处理未向量的 Post
     */
    public void vectorizePendingPosts() {
        try {
            List<Map<String, Object>> pendingPosts = fetchPendingPosts();
            if (pendingPosts.isEmpty()) {
                log.info("没有待向量化的 Post");
                return;
            }

            log.info("开始批量向量化，待处理数量: {}", pendingPosts.size());

            // 分批处理
            for (int i = 0; i < pendingPosts.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, pendingPosts.size());
                List<Map<String, Object>> batch = pendingPosts.subList(i, end);
                processBatch(batch);
            }

            log.info("批量向量化完成，处理数量: {}", pendingPosts.size());

        } catch (Exception e) {
            log.error("批量向量化失败", e);
        }
    }

    /**
     * 获取未向量化的 Post（embedding 为 null）
     */
    private List<Map<String, Object>> fetchPendingPosts() throws Exception {
        // 查询条件：embedding 不存在 且 status = 1（正常状态）
        Query query = BoolQuery.of(q -> q
                .mustNot(m -> m.exists(e -> e.field("embedding")))
                .must(m -> m.term(t -> t.field("status").value(1)))
        )._toQuery();

        SearchRequest request = SearchRequest.of(s -> s
                .index(POST_INDEX)
                .query(query)
                .size(1000)  // 单次最多处理 1000 条
        );

        SearchResponse<Map<String, Object>> response = elasticsearchClient.search(request, new TypeReference<Map<String, Object>>() {});

        return response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());
    }

    /**
     * 处理一批 Post
     */
    private void processBatch(List<Map<String, Object>> batch) throws Exception {
        // 1. 提取文本内容：title + content + labels
        List<String> texts = batch.stream()
                .map(this::extractText)
                .collect(Collectors.toList());

        // 2. 批量调用 DashScope 生成向量
        EmbeddingResponse embeddingResponse = embeddingModel.embedForResponse(texts);

        // 3. 更新 ES 文档
        List<float[]> embeddings = embeddingResponse.getResults().stream()
                .map(r -> r.getOutput())
                .collect(Collectors.toList());

        for (int i = 0; i < batch.size(); i++) {
            Map<String, Object> doc = batch.get(i);
            Long id = (Long) doc.get("id");
            float[] embedding = embeddings.get(i);

            updatePostEmbedding(id, embedding);
        }
    }

    /**
     * 提取文本内容
     */
    private String extractText(Map<String, Object> doc) {
        StringBuilder text = new StringBuilder();

        String title = (String) doc.get("title");
        if (title != null) {
            text.append(title).append(" ");
        }

        String content = (String) doc.get("content");
        if (content != null) {
            text.append(content).append(" ");
        }

        @SuppressWarnings("unchecked")
        List<String> labels = (List<String>) doc.get("labels");
        if (labels != null && !labels.isEmpty()) {
            text.append(String.join(" ", labels));
        }

        return text.toString();
    }

    /**
     * 更新 Post 的 embedding 字段
     */
    private void updatePostEmbedding(Long id, float[] embedding) throws Exception {
        Map<String, Object> partialDoc = new HashMap<>();
        partialDoc.put("embedding", embedding);
        partialDoc.put("embedded_at", System.currentTimeMillis());

        elasticsearchClient.update(u -> u
                        .index(POST_INDEX)
                        .id(String.valueOf(id))
                        .doc(partialDoc),
                Void.class);

        log.debug("Post 向量化完成: id={}", id);
    }
}
