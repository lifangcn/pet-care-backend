package pvt.mktech.petcare.infrastructure.config;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * {@code @description}: AI 服务定时任务配置，使用专用线程池
 * {@code @date}: 2026/02/05
 *
 * @author Michael
 */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerConfig implements SchedulingConfigurer {

    @Resource
    private ScheduledThreadPoolExecutor aiScheduledThreadPoolExecutor;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(aiScheduledThreadPoolExecutor);
    }
}
