package pvt.mktech.petcare.chat.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.sync.constants.EsIndexConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pvt.mktech.petcare.sync.constants.SyncConstants.ACTIVITY_INDEX;
import static pvt.mktech.petcare.sync.constants.SyncConstants.POST_INDEX;

/**
 * {@code @description}: 全文检索服务（基于 BM25）
 * <p>Post/Activity 采用纯 BM25 检索，满足关键词匹配场景</p>
 * {@code @date}: 2026-02-05
 * @author Michael
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchService {

    private final ElasticsearchClient elasticsearchClient;

    /**
     * 检索 Post（BM25）
     */
    public List<Map<String, Object>> hybridSearchPosts(String query, int size, double minScore) {
        return bm25Search(POST_INDEX, query, List.of("title^2", "content"), size, minScore);
    }

    /**
     * 检索 Activity（BM25）
     */
    public List<Map<String, Object>> hybridSearchActivities(String query, int size, double minScore) {
        return bm25Search(ACTIVITY_INDEX, query, List.of("title^2", "description", "address"), size, minScore);
    }

    /**
     * BM25 全文检索核心方法
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Map<String, Object>> bm25Search(String index, String query, List<String> fields, int size, double minScore) {
        try {
            Query bm25Query = MultiMatchQuery.of(m -> m
                    .query(query)
                    .fields(fields)
                    .type(TextQueryType.BestFields)
            )._toQuery();

            SearchRequest request = SearchRequest.of(s -> s
                    .index(index)
                    .query(bm25Query)
                    .size(size)
                    .minScore(minScore)
            );

            SearchResponse<Map> response = elasticsearchClient.search(request, Map.class);

            List<Map<String, Object>> results = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    Map<String, Object> result = new HashMap<>(hit.source());
                    result.put("_score", hit.score());
                    results.add(result);
                }
            }
            return results;
        } catch (Exception e) {
            log.error("BM25 检索失败: index={}, query={}", index, query, e);
            return List.of();
        }
    }
}
