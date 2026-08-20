package pvt.mktech.petcare.observability.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.agent.telemetry.mapper.AgentExecutionTelemetryMapper;
import pvt.mktech.petcare.observability.config.TelemetryProperties;
import pvt.mktech.petcare.observability.mapper.ChatTraceMapper;

/** Removes expired PostgreSQL telemetry in small lock-skipping batches. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "petcare.telemetry", name = "store", havingValue = "postgresql")
public class TelemetryRetentionCleanupScheduler {
    private final ChatTraceMapper chatTraceMapper;
    private final AgentExecutionTelemetryMapper agentExecutionTelemetryMapper;
    private final TelemetryProperties telemetryProperties;

    @Scheduled(cron = "${petcare.telemetry.cleanup.cron:0 0 * * * *}")
    public void cleanupExpiredTelemetry() {
        cleanup("chat traces", chatTraceMapper::deleteExpiredBatch);
        cleanup("agent executions", agentExecutionTelemetryMapper::deleteExpiredBatch);
    }

    private void cleanup(String table, BatchDeleter deleter) {
        int deleted = 0;
        try {
            for (int batch = 0; batch < telemetryProperties.getCleanup().getMaxBatches(); batch++) {
                int count = deleter.delete(telemetryProperties.getCleanup().getBatchSize());
                deleted += count;
                if (count < telemetryProperties.getCleanup().getBatchSize()) {
                    break;
                }
            }
            log.info("Telemetry cleanup deleted {} expired {}", deleted, table);
        } catch (RuntimeException exception) {
            log.warn("Telemetry cleanup failed for {} after deleting {} rows", table, deleted, exception);
        }
    }

    @FunctionalInterface
    private interface BatchDeleter {
        int delete(int batchSize);
    }
}
