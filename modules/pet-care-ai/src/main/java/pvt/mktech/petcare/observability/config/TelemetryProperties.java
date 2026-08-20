package pvt.mktech.petcare.observability.config;

import lombok.Data;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Chat trace storage configuration. */
@Data
@Validated
@ConfigurationProperties(prefix = "petcare.telemetry")
public class TelemetryProperties {

    private String store = "elasticsearch";
    @Min(1)
    private int retentionDays = 30;
    private Cleanup cleanup = new Cleanup();

    @Data
    public static class Cleanup {
        @Min(1)
        private int batchSize = 500;
        @Min(1)
        private int maxBatches = 5;
        private String cron = "0 0 * * * *";
    }
}
