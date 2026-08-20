package pvt.mktech.petcare.observability.store;

import pvt.mktech.petcare.observability.dto.ChatTraceDocument;

/** Persists an individual chat trace. */
public interface ChatTraceStore {

    void save(ChatTraceDocument document);
}
