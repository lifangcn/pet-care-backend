package pvt.mktech.petcare.chat.tool;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.KnnQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.chat.dto.SearchResult;
import pvt.mktech.petcare.sync.constants.EsIndexConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * {@code @description}: 多索引统一检索工具
 * 通过 Function Calling 让 AI 自主检索知识库、Post、Activity
 * 知识库使用向量检索，Post/Activity 使用 BM25 检索
 * {@code @date}: 2026-02-05
 * @author Michael
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MultiIndexSearchTool {

    private final ElasticsearchClient elasticsearchClient;
    private final EmbeddingModel embeddingModel;

    /**
     * 检索知识库文档（KNN 向量检索）
     */
    @Tool(name = "searchKnowledgeDocuments",
            description = "检索知识库文档，获取宠物医疗、护理、训练等专业知识")
    public List<SearchResult> searchKnowledge(KnowledgeSearchRequest request) {
        log.info("AI 调用知识库检索: query={}, topK={}", request.query(), request.topK());
        int topK = request.topK() != null && request.topK() > 0 ? Math.min(request.topK(), 10) : 5;
        return knnSearch(EsIndexConstants.KNOWLEDGE_DOCUMENT_INDEX, request.query(), topK);
    }

    /**
     * 检索用户动态（BM25 全文检索）
     */
    @Tool(name = "searchUserPosts",
            description = "检索用户动态，获取好物分享、服务推荐、地点推荐等实际经验")
    public List<SearchResult> searchPosts(PostSearchRequest request) {
        log.info("AI 调用动态检索: query={}, topK={}", request.query(), request.topK());
        int topK = request.topK() != null && request.topK() > 0 ? Math.min(request.topK(), 10) : 5;
        return bm25Search(EsIndexConstants.POST_INDEX, request.query(), List.of("title^2", "content"), topK, "post");
    }

    /**
     * 检索活动信息（BM25 全文检索）
     */
    @Tool(name = "searchActivities",
            description = "检索宠物活动，获取线上线下聚会、遛狗活动等信息")
    public List<SearchResult> searchActivities(ActivitySearchRequest request) {
        log.info("AI 调用活动检索: query={}, topK={}", request.query(), request.topK());
        int topK = request.topK() != null && request.topK() > 0 ? Math.min(request.topK(), 10) : 5;
        return bm25Search(EsIndexConstants.ACTIVITY_INDEX, request.query(), List.of("title^2", "description", "address"), topK, "activity");
    }

    /**
     * 知识库检索请求参数
     */
    public record KnowledgeSearchRequest(
            @Description("查询文本，例如：狗狗发烧怎么办、猫咪疫苗接种时间") String query,
            @Description("返回条数，默认5条，最多10条") Integer topK
    ) {}

    /**
     * 动态检索请求参数
     */
    public record PostSearchRequest(
            @Description("查询文本，例如：宠物医院推荐、狗粮品牌") String query,
            @Description("返回条数，默认5条，最多10条") Integer topK
    ) {}

    /**
     * 活动检索请求参数
     */
    public record ActivitySearchRequest(
            @Description("查询文本，例如：周末宠物活动、遛狗聚会") String query,
            @Description("返回条数，默认5条，最多10条") Integer topK
    ) {}

    /**
     * KNN 向量检索（用于知识库）
     */
    private List<SearchResult> knnSearch(String index, String query, int size) {
        try {
            float[] queryVector = generateQueryVector(query);
            List<Float> vectorList = toFloatList(queryVector);

            Query knnQuery = KnnQuery.of(k -> k
                    .field("embedding")
                    .queryVector(vectorList)
                    .k(size)
                    .numCandidates(size * 2)
            )._toQuery();

            return executeSearch(index, knnQuery, size, "knowledge", "document", 0.1);
        } catch (Exception e) {
            log.error("知识库 KNN 检索失败: query={}", query, e);
            return List.of();
        }
    }

    /**
     * BM25 全文检索（用于 Post/Activity）
     */
    private List<SearchResult> bm25Search(String index, String query, List<String> fields, int size, String source) {
        try {
            Query bm25Query = MultiMatchQuery.of(m -> m
                    .query(query)
                    .fields(fields)
                    .type(TextQueryType.BestFields)
            )._toQuery();

            return executeSearch(index, bm25Query, size, source, source, 0.2);
        } catch (Exception e) {
            log.error("BM25 检索失败: index={}, query={}", index, query, e);
            return List.of();
        }
    }

    /**
     * 执行 ES 查询
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<SearchResult> executeSearch(String index, Query query, int size,
                                             String source, String type, Double minScore) {
        try {
            SearchRequest.Builder builder = new SearchRequest.Builder()
                    .index(index)
                    .query(query)
                    .size(size);

            if (minScore != null) {
                builder.minScore(minScore);
            }

            SearchResponse<Map> response = elasticsearchClient.search(builder.build(), Map.class);

            List<SearchResult> results = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                if (hit.source() == null) continue;

                Map<String, Object> sourceMap = hit.source();
                SearchResult.SearchResultBuilder resultBuilder = SearchResult.builder()
                        .source(source)
                        .type(type)
                        .score(hit.score());

                // 根据索引类型提取字段
                switch (source) {
                    case "knowledge" -> {
                        resultBuilder.title((String) sourceMap.get("name"));
                        resultBuilder.content((String) sourceMap.get("content"));
                    }
                    case "post" -> {
                        resultBuilder.title((String) sourceMap.get("title"));
                        resultBuilder.content((String) sourceMap.get("content"));
                    }
                    case "activity" -> {
                        resultBuilder.title((String) sourceMap.get("title"));
                        resultBuilder.content((String) sourceMap.get("description"));
                    }
                }

                // 提取元数据（排除已用字段）
                Map<String, Object> metadata = new HashMap<>();
                for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
                    String key = entry.getKey();
                    if (!isExcludedField(key)) {
                        metadata.put(key, entry.getValue());
                    }
                }
                resultBuilder.metadata(metadata);

                results.add(resultBuilder.build());
            }

            log.info("检索完成: index={}, 结果数={}", index, results.size());
            return results;

        } catch (Exception e) {
            log.error("执行 ES 查询失败: index={}", index, e);
            return List.of();
        }
    }

    /**
     * 判断是否为需要排除的字段
     */
    private boolean isExcludedField(String key) {
        return Stream.of("embedding", "title", "content", "description", "name")
                .anyMatch(key::equals);
    }

    /**
     * 生成查询向量
     */
    private float[] generateQueryVector(String query) {
        try {
            var response = embeddingModel.embedForResponse(List.of(query));
            return response.getResults().getFirst().getOutput();
        } catch (Exception e) {
            log.error("生成查询向量失败", e);
            return new float[1024];
        }
    }

    /**
     * float[] 转 List<Float>
     */
    private List<Float> toFloatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float f : array) {
            list.add(f);
        }
        return list;
    }
}
