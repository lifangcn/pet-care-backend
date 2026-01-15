package pvt.mktech.petcare.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@code @description}:
 * {@code @date}: 2026/1/15 13:31
 *
 * @author Michael
 */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "scheduler", name = "enabled", havingValue = "true")
public class SchedulerConfig {

}
