package pvt.mktech.petcare.sync.constants;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

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
}
