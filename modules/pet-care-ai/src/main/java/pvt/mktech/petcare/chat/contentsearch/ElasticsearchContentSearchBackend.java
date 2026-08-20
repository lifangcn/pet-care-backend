package pvt.mktech.petcare.chat.contentsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.chat.dto.SearchResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pvt.mktech.petcare.sync.constants.SyncConstants.ACTIVITY_INDEX;
import static pvt.mktech.petcare.sync.constants.SyncConstants.POST_INDEX;

/** Legacy Elasticsearch BM25 content-search backend. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "petcare.search.content-backend", havingValue = "elasticsearch")
public class ElasticsearchContentSearchBackend implements ContentSearchBackend {
    private final ElasticsearchClient elasticsearchClient;

    @Override
    public List<SearchResult> searchPosts(ContentSearchQuery query) {
        return search(POST_INDEX, query.query(), List.of("title^2", "content"), query.topK(), "post", null, null);
    }

    @Override
    public List<SearchResult> searchActivities(ContentSearchQuery query) {
        return search(ACTIVITY_INDEX, query.query(), List.of("title^2", "description", "address"), query.topK(), "activity",
                query.startTime() == null ? null : query.startTime().toString(), query.endTime() == null ? null : query.endTime().toString());
    }

    private List<SearchResult> search(String index, String queryText, List<String> fields, int topK, String source,
                                      String startTime, String endTime) {
        try {
            Query bm25 = MultiMatchQuery.of(query -> query.query(queryText).fields(fields).type(TextQueryType.BestFields))._toQuery();
            Query finalQuery = (startTime == null && endTime == null) ? bm25 : Query.of(query -> query.bool(bool -> bool.must(bm25)
                    .filter(co.elastic.clients.elasticsearch._types.query_dsl.RangeQueryBuilders.date(date -> {
                        var range = date.field("activity_time");
                        if (startTime != null) range.gte(startTime);
                        if (endTime != null) range.lte(endTime);
                        return range;
                    }))));
            SearchResponse<Map> response = elasticsearchClient.search(new co.elastic.clients.elasticsearch.core.SearchRequest.Builder()
                    .index(index).query(finalQuery).size(topK).minScore(0.2).build(), Map.class);
            List<SearchResult> results = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                if (hit.source() == null) continue;
                Map<String, Object> document = hit.source();
                String content = "post".equals(source) ? stringValue(document.get("content")) : stringValue(document.get("description"));
                results.add(SearchResult.builder().source(source).type(source).title(stringValue(document.get("title")))
                        .content(content).score(hit.score()).metadata(metadata(document)).build());
            }
            return results;
        } catch (Exception exception) {
            log.error("内容检索失败: backend=elasticsearch, query={}", queryText, exception);
            return List.of();
        }
    }

    private String stringValue(Object value) { return value == null ? null : String.valueOf(value); }
    private Map<String, Object> metadata(Map<String, Object> document) {
        Map<String, Object> metadata = new HashMap<>(document);
        metadata.remove("embedding"); metadata.remove("title"); metadata.remove("content"); metadata.remove("description"); metadata.remove("name");
        return metadata;
    }
}
