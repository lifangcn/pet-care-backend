package pvt.mktech.petcare.core.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pvt.mktech.petcare.common.thread.ThreadPoolManager;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class CoreThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor coreThreadPoolExecutor(MeterRegistry meterRegistry) {
        ThreadPoolExecutor threadPoolExecutor = ThreadPoolManager.createThreadPool("core-service");
        ExecutorServiceMetrics.monitor(meterRegistry, threadPoolExecutor, "core-service");
        // 预热线程
        threadPoolExecutor.prestartAllCoreThreads();
        return threadPoolExecutor;
    }

    @Bean
    public ScheduledThreadPoolExecutor coreScheduledThreadPoolExecutor(MeterRegistry meterRegistry) {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = ThreadPoolManager.createScheduledThreadPool("core-scheduled");
        ExecutorServiceMetrics.monitor(meterRegistry, scheduledThreadPoolExecutor, "core-scheduled");
        return scheduledThreadPoolExecutor;
    }

    @Bean
    public ScheduledThreadPoolExecutor sseHeartbeatThreadPoolExecutor(MeterRegistry meterRegistry) {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = ThreadPoolManager.createScheduledThreadPool("core-sse-heartbeat");
        ExecutorServiceMetrics.monitor(meterRegistry, scheduledThreadPoolExecutor, "core-sse-heartbeat");
        return scheduledThreadPoolExecutor;
    }

    // 监控指标注入
//    @Bean
//    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
//        return registry -> registry.config().commonTags("application", "pet-care-core");
//    }
}