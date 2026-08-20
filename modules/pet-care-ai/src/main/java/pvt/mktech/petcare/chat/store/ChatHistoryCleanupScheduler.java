package pvt.mktech.petcare.chat.store;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.chat.repository.ChatHistoryRepository;
import pvt.mktech.petcare.infrastructure.config.ChatMemoryProperties;

@Slf4j @Component @RequiredArgsConstructor
@ConditionalOnProperty(prefix="spring.ai.chat.memory.history",name="enabled",havingValue="true",matchIfMissing=true)
public class ChatHistoryCleanupScheduler {
    private final ChatHistoryRepository repository;
    private final ChatMemoryProperties properties;
    @Scheduled(cron="${spring.ai.chat.memory.history.cleanup-cron:0 0 * * * *}")
    public void cleanup() {
        int batchSize = properties.getHistory().getCleanupBatchSize();
        try {
            for (int i = 0; i < properties.getHistory().getCleanupMaxBatches(); i++) {
                int deleted = repository.deleteExpiredBatch(batchSize);
                if (deleted < batchSize) break;
            }
        } catch (RuntimeException exception) {
            log.warn("Chat history cleanup failed: batchSize={}, maxBatches={}", batchSize,
                    properties.getHistory().getCleanupMaxBatches(), exception);
        }
    }
}
