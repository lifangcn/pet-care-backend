package pvt.mktech.petcare.core.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * {@code @description}: 定时任务配置，使用专用线程池
 * {@code @date}: 2026/1/15 13:31
 *
 * @author Michael
 */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "scheduler", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class SchedulerConfig implements SchedulingConfigurer {
    private final ScheduledThreadPoolExecutor coreScheduledThreadPoolExecutor;
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // 使用 2 个线程的线程池，让 reminderScanJob 和 delayQueueScanJob 可并行执行
        taskRegistrar.setScheduler(coreScheduledThreadPoolExecutor);
    }
}
