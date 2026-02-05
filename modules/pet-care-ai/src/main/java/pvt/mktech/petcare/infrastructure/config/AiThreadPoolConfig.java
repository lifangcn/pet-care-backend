package pvt.mktech.petcare.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pvt.mktech.petcare.common.thread.ThreadPoolManager;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AiThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor aiThreadPoolExecutor(MeterRegistry meterRegistry) {
        ThreadPoolExecutor threadPoolExecutor = ThreadPoolManager.createThreadPool("ai-service");
        ExecutorServiceMetrics.monitor(meterRegistry, threadPoolExecutor, "ai-service");
        // 预热线程
        threadPoolExecutor.prestartAllCoreThreads();
        return threadPoolExecutor;
    }

    /**
     * AI 服务定时任务线程池
     */
    @Bean
    public ScheduledThreadPoolExecutor aiScheduledThreadPoolExecutor(MeterRegistry meterRegistry) {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = ThreadPoolManager.createScheduledThreadPool("ai-scheduled");
        ExecutorServiceMetrics.monitor(meterRegistry, scheduledThreadPoolExecutor, "ai-scheduled");
        // 关闭后继续执行已存在的延迟任务
        scheduledThreadPoolExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(true);
        // 关闭后继续执行已存在的周期性任务
        scheduledThreadPoolExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(true);
        // 取消任务时从队列中移除
        scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true);
        return scheduledThreadPoolExecutor;
    }

//    @Bean
//    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
//        return registry -> registry.config().commonTags("application", "pet-care-ai");
//    }
}