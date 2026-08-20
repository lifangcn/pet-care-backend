package pvt.mktech.petcare.sync.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.ElasticsearchTransport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexAdminServiceTest {

    @ParameterizedTest(name = "knowledge={0}, telemetry={1}, history={2}")
    @MethodSource("storeCombinations")
    void initAllIndices_shouldApplyEachIndexStorePolicyIndependently(
            String vectorStoreType, String telemetryStore, String chatHistoryStore,
            boolean knowledgeIndexExpected, boolean telemetryIndicesExpected, boolean chatHistoryIndexExpected) {
        var service = createService(vectorStoreType, telemetryStore, chatHistoryStore);

        service.initAllIndices();

        assertEquals(knowledgeIndexExpected, service.knowledgeDocumentIndexCreated);
        assertTrue(service.postIndexCreated);
        assertTrue(service.activityIndexCreated);
        assertEquals(chatHistoryIndexExpected, service.chatHistoryIndexCreated);
        assertEquals(telemetryIndicesExpected, service.chatTraceIndexCreated);
        assertEquals(telemetryIndicesExpected, service.agentExecutionIndexCreated);
    }

    @Test
    void createChatHistoryIndex_shouldNoOp_whenHistoryStoreIsPostgresql() {
        var service = createService("elasticsearch", "elasticsearch", "POSTGRESQL");

        assertTrue(service.createChatHistoryIndex());

        assertFalse(service.chatHistoryIndexCreated);
    }

    private static Stream<Arguments> storeCombinations() {
        return Stream.of(
                Arguments.of("elasticsearch", "elasticsearch", "elasticsearch", true, true, true),
                Arguments.of("pgvector", "elasticsearch", "elasticsearch", false, true, true),
                Arguments.of("elasticsearch", "postgresql", "elasticsearch", true, false, true),
                Arguments.of("elasticsearch", "elasticsearch", "postgresql", true, true, false),
                Arguments.of("pgvector", "postgresql", "elasticsearch", false, false, true),
                Arguments.of("pgvector", "elasticsearch", "postgresql", false, true, false),
                Arguments.of("elasticsearch", "postgresql", "postgresql", true, false, false),
                Arguments.of("pgvector", "postgresql", "postgresql", false, false, false)
        );
    }

    private TrackingIndexAdminService createService(String vectorStoreType, String telemetryStore, String chatHistoryStore) {
        return new TrackingIndexAdminService(vectorStoreType, telemetryStore, chatHistoryStore);
    }

    private static class TrackingIndexAdminService extends IndexAdminService {

        private boolean knowledgeDocumentIndexCreated;
        private boolean postIndexCreated;
        private boolean activityIndexCreated;
        private boolean chatHistoryIndexCreated;
        private boolean chatTraceIndexCreated;
        private boolean agentExecutionIndexCreated;

        private TrackingIndexAdminService(String vectorStoreType, String telemetryStore, String chatHistoryStore) {
            super(new ElasticsearchClient((ElasticsearchTransport) null), vectorStoreType, telemetryStore, chatHistoryStore);
        }

        @Override
        public boolean createIndexFromMapping(String indexName, String mappingJson) {
            switch (indexName) {
                case "knowledge_document" -> knowledgeDocumentIndexCreated = true;
                case "post" -> postIndexCreated = true;
                case "activity" -> activityIndexCreated = true;
                case "chat_history" -> chatHistoryIndexCreated = true;
                case "chat_trace" -> chatTraceIndexCreated = true;
                case "agent_execution" -> agentExecutionIndexCreated = true;
                default -> throw new IllegalArgumentException("Unexpected index: " + indexName);
            }
            return true;
        }
    }
}
