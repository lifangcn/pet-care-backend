package pvt.mktech.petcare.observability.store;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import pvt.mktech.petcare.observability.dto.ChatTraceDocument;

/** Elasticsearch-backed chat trace store. */
@ConditionalOnProperty(prefix = "petcare.telemetry", name = "store", havingValue = "elasticsearch", matchIfMissing = true)
public class ElasticsearchChatTraceStore implements ChatTraceStore {

    private final ElasticsearchClient elasticsearchClient;
    private final String indexName;

    public ElasticsearchChatTraceStore(ElasticsearchClient elasticsearchClient, String indexName) {
        this.elasticsearchClient = elasticsearchClient;
        this.indexName = indexName;
    }

    @Override
    public void save(ChatTraceDocument document) {
        IndexRequest<ChatTraceDocument> request = buildRequest(document);
        try {
            elasticsearchClient.index(request);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to store chat trace in Elasticsearch", exception);
        }
    }

    IndexRequest<ChatTraceDocument> buildRequest(ChatTraceDocument document) {
        return IndexRequest.of(requestBuilder -> requestBuilder
                .index(indexName)
                .id(document.getTraceId())
                .document(document));
    }
}
