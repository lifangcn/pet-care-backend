package pvt.mktech.petcare.sync.constants;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import pvt.mktech.petcare.entity.ChatMessageDocument;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EsIndexMappingsTest {

    @Test
    void mappingsDoNotRequireUnavailableIkAnalyzer() {
        String allMappings = String.join("\n",
                EsIndexMappings.KNOWLEDGE_DOCUMENT_MAPPING,
                EsIndexMappings.POST_MAPPING,
                EsIndexMappings.ACTIVITY_MAPPING,
                EsIndexMappings.CHAT_HISTORY_MAPPING,
                EsIndexMappings.CHAT_TRACE_MAPPING,
                EsIndexMappings.AGENT_EXECUTION_MAPPING);

        assertFalse(allMappings.contains("ik_max_word"));
    }

    @Test
    void chatHistoryMappingContainsMessageAndSessionFields() {
        String mapping = EsIndexMappings.CHAT_HISTORY_MAPPING;

        assertTrue(mapping.contains("\"document_type\""));
        assertTrue(mapping.contains("\"name\""));
        assertTrue(mapping.contains("\"updated_at\""));
        assertTrue(mapping.contains("\"message_count\""));
        assertTrue(mapping.contains("\"expires_at\""));
    }

    @Test
    void chatMessagesSerializeUsingExplicitSnakeCaseProperties() throws Exception {
        ChatMessageDocument message = new ChatMessageDocument();
        message.setId(1L);
        message.setUserId(2L);
        message.setSessionId("session");
        message.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        String json = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(message);

        assertTrue(json.contains("\"document_type\":\"message\""));
        assertTrue(json.contains("\"user_id\":2"));
        assertTrue(json.contains("\"session_id\":\"session\""));
        assertTrue(json.contains("\"created_at\""));
    }
}
