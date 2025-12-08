package pvt.mktech;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.util.ObjectBuilder;
import jakarta.annotation.Resource;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@code @description}: 测试
 * {@code @date}: 2025/12/5 13:19
 *
 * @author Michael
 */
@Service
public class ElasticsearchService {
    @Resource
    private ElasticsearchTemplate elasticsearchTemplate;

    // 创建索引
    public void createIndexWithMapping(String indexName, TypeMapping mapping) {
        // 检查索引是否存在
        ExistsRequest existsRequest = ExistsRequest.of(builder -> builder.index(indexName));
        boolean exists = elasticsearchTemplate.execute(client ->
                client.indices().exists(existsRequest)).value();

        if (!exists) {
            CreateIndexRequest createIndexRequest = CreateIndexRequest.of(builder -> builder.index(indexName).mappings(mapping));
            elasticsearchTemplate.execute(client -> client.indices().create(createIndexRequest));
        }
    }

    // 删除索引
    public void deleteIndex(String indexName) {
        DeleteIndexRequest deleteIndexRequest = DeleteIndexRequest.of(builder -> builder.index(indexName));
        elasticsearchTemplate.execute(client -> client.indices().delete(deleteIndexRequest));
    }

    // 创建文档
    public <T> void createDocument(String indexName, String documentId, T document) {
        IndexRequest<T> indexRequest = IndexRequest.of(builder -> builder.index(indexName).id(documentId).document(document));
        elasticsearchTemplate.execute(client -> client.index(indexRequest));
    }

    // 查询单个文档
    public <T> T getDocument(String indexName, String documentId, Class<T> clazz) {
        GetRequest getRequest = GetRequest.of(builder -> builder.index(indexName).id(documentId));
        GetResponse<T> response = elasticsearchTemplate.execute(client -> client.get(getRequest, clazz));
        return response.found() ? response.source() : null;
    }

    // 更新文档
    public <T> void updateDocument(String indexName, String documentId, T document) {
        UpdateRequest.Builder<T, T> builder = new UpdateRequest.Builder<>();
        UpdateRequest<T, T> request = builder.index(indexName).id(documentId).doc(document).build();
        UpdateResponse<T> response = elasticsearchTemplate.execute(client -> client.update(request, document.getClass()));
    }

    // 删除文档
    public void deleteDocument(String indexName, String documentId) {
        DeleteRequest deleteRequest = DeleteRequest.of(builder -> builder.index(indexName).id(documentId));
        DeleteResponse execute = elasticsearchTemplate.execute(client -> client.delete(deleteRequest));
    }

    // 搜索文档
    public <T> List<T> searchDocuments(String indexName, String query, Class<T> clazz) {
        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder.index(indexName);
        builder.query(getBuilderObjectBuilderFunction("description", query));
        builder.highlight(getBuilderObjectBuilderFunction("description"));
        SearchRequest searchRequest = builder.build();
        SearchResponse<T> response = elasticsearchTemplate.execute(client -> client.search(searchRequest, clazz));
        return response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
    }

    public <T> List<T> searchDocumentWithHighlight(String indexName, String fieldName, String query, Class<T> clazz) {
        // 构建高亮查询请求
        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder.index(indexName);
        builder.query(getBuilderObjectBuilderFunction(fieldName, query));
        builder.highlight(getBuilderObjectBuilderFunction(fieldName));
        // 执行查询
        SearchRequest searchRequest = builder.build();
        SearchResponse<T> response = elasticsearchTemplate.execute(client -> client.search(searchRequest, clazz));
        // 处理返回结果，高亮内容覆盖原文本


        return response.hits().hits().stream().map(hit -> {
            T source = hit.source();
            Map<String, List<String>> highlightMaps = hit.highlight();
            if (highlightMaps != null && highlightMaps.containsKey(fieldName)) {
                List<String> highlights = highlightMaps.get(fieldName);
                if (highlights != null && !highlights.isEmpty()) {
                    // 这里需要根据具体的实体类结构来设置高亮内容
                    // 示例：假设实体类有 setter 方法
                    // source.setFieldName(highlights.get(0));
                }
            }
            return source;
        }).collect(Collectors.toList());
    }

    private static Function<Highlight.Builder, ObjectBuilder<Highlight>> getBuilderObjectBuilderFunction(String fieldName) {
        return h -> h.fields(fieldName, hf -> hf.preTags("<em>").postTags("</em>"));
    }

    private static Function<Query.Builder, ObjectBuilder<Query>> getBuilderObjectBuilderFunction(String fieldName, String query) {
        return q -> q.match(match -> match.field(fieldName).query(query));
    }

}
