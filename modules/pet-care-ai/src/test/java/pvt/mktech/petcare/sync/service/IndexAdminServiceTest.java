package pvt.mktech.petcare.sync.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.ElasticsearchTransport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class IndexAdminServiceTest {

    @Test
    void initAllIndices_shouldSkipKnowledgeDocumentIndex_whenVectorStoreTypeIsPgvector() {
        var service = createService("pgvector");

        service.initAllIndices();

        verify(service, never()).createKnowledgeDocumentIndex();
        verifyOtherIndicesCreated(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"elasticsearch", "Elasticsearch"})
    void initAllIndices_shouldCreateKnowledgeDocumentIndex_whenVectorStoreTypeIsElasticsearch(String vectorStoreType) {
        var service = createService(vectorStoreType);

        service.initAllIndices();

        verify(service).createKnowledgeDocumentIndex();
        verifyOtherIndicesCreated(service);
    }

    private IndexAdminService createService(String vectorStoreType) {
        var service = spy(new IndexAdminService(new ElasticsearchClient((ElasticsearchTransport) null), vectorStoreType));
        doReturn(true).when(service).createKnowledgeDocumentIndex();
        doReturn(true).when(service).createPostIndex();
        doReturn(true).when(service).createActivityIndex();
        doReturn(true).when(service).createChatHistoryIndex();
        doReturn(true).when(service).createChatTraceIndex();
        doReturn(true).when(service).createAgentExecutionIndex();
        return service;
    }

    private void verifyOtherIndicesCreated(IndexAdminService service) {
        verify(service).createPostIndex();
        verify(service).createActivityIndex();
        verify(service).createChatHistoryIndex();
        verify(service).createChatTraceIndex();
        verify(service).createAgentExecutionIndex();
    }
}
