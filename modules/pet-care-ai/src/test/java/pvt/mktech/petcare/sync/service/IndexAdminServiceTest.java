package pvt.mktech.petcare.sync.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.ElasticsearchTransport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexAdminServiceTest {

    @Test
    void initAllIndices_shouldCreateAllIndices_whenVectorAndTelemetryStoresAreElasticsearch() {
        var service = createService("Elasticsearch", "ELASTICSEARCH");

        service.initAllIndices();

        assertTrue(service.knowledgeDocumentIndexCreated);
        verifyAlwaysCreatedIndices(service);
        verifyTelemetryIndicesCreated(service);
    }

    @Test
    void initAllIndices_shouldSkipOnlyKnowledgeIndex_whenVectorStoreIsPgvectorAndTelemetryStoreIsElasticsearch() {
        var service = createService("PGVECTOR", "elasticsearch");

        service.initAllIndices();

        assertFalse(service.knowledgeDocumentIndexCreated);
        verifyAlwaysCreatedIndices(service);
        verifyTelemetryIndicesCreated(service);
    }

    @Test
    void initAllIndices_shouldSkipKnowledgeAndTelemetryIndices_whenVectorStoreIsPgvectorAndTelemetryStoreIsPostgresql() {
        var service = createService("pgvector", "POSTGRESQL");

        service.initAllIndices();

        assertFalse(service.knowledgeDocumentIndexCreated);
        verifyAlwaysCreatedIndices(service);
        verifyTelemetryIndicesSkipped(service);
    }

    @Test
    void initAllIndices_shouldSkipOnlyTelemetryIndices_whenVectorStoreIsElasticsearchAndTelemetryStoreIsPostgresql() {
        var service = createService("elasticsearch", "PostgreSQL");

        service.initAllIndices();

        assertTrue(service.knowledgeDocumentIndexCreated);
        verifyAlwaysCreatedIndices(service);
        verifyTelemetryIndicesSkipped(service);
    }

    private TrackingIndexAdminService createService(String vectorStoreType, String telemetryStore) {
        return new TrackingIndexAdminService(vectorStoreType, telemetryStore);
    }

    private void verifyAlwaysCreatedIndices(TrackingIndexAdminService service) {
        assertTrue(service.postIndexCreated);
        assertTrue(service.activityIndexCreated);
        assertTrue(service.chatHistoryIndexCreated);
    }

    private void verifyTelemetryIndicesCreated(TrackingIndexAdminService service) {
        assertTrue(service.chatTraceIndexCreated);
        assertTrue(service.agentExecutionIndexCreated);
    }

    private void verifyTelemetryIndicesSkipped(TrackingIndexAdminService service) {
        assertFalse(service.chatTraceIndexCreated);
        assertFalse(service.agentExecutionIndexCreated);
    }

    private static class TrackingIndexAdminService extends IndexAdminService {

        private boolean knowledgeDocumentIndexCreated;
        private boolean postIndexCreated;
        private boolean activityIndexCreated;
        private boolean chatHistoryIndexCreated;
        private boolean chatTraceIndexCreated;
        private boolean agentExecutionIndexCreated;

        private TrackingIndexAdminService(String vectorStoreType, String telemetryStore) {
            super(new ElasticsearchClient((ElasticsearchTransport) null), vectorStoreType, telemetryStore);
        }

        @Override
        public boolean createKnowledgeDocumentIndex() {
            knowledgeDocumentIndexCreated = true;
            return true;
        }

        @Override
        public boolean createPostIndex() {
            postIndexCreated = true;
            return true;
        }

        @Override
        public boolean createActivityIndex() {
            activityIndexCreated = true;
            return true;
        }

        @Override
        public boolean createChatHistoryIndex() {
            chatHistoryIndexCreated = true;
            return true;
        }

        @Override
        public boolean createChatTraceIndex() {
            chatTraceIndexCreated = true;
            return true;
        }

        @Override
        public boolean createAgentExecutionIndex() {
            agentExecutionIndexCreated = true;
            return true;
        }
    }
}
