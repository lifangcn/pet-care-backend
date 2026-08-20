package pvt.mktech.petcare.observability.store;

import co.elastic.clients.elasticsearch.core.IndexRequest;
import org.junit.jupiter.api.Test;
import pvt.mktech.petcare.observability.dto.ChatTraceDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElasticsearchChatTraceStoreTest {

    @Test
    void buildsRequestUsingConfiguredIndexAndTraceId() {
        ChatTraceDocument document = new ChatTraceDocument();
        document.setTraceId("b4f1e60b-f1b5-412d-93d2-1712f7633263");

        IndexRequest<ChatTraceDocument> request =
                new ElasticsearchChatTraceStore(null, "chat_trace").buildRequest(document);

        assertEquals("chat_trace", request.index());
        assertEquals(document.getTraceId(), request.id());
        assertEquals(document, request.document());
    }
}
