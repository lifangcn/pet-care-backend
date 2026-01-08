package pvt.mktech.petcare.core.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pvt.mktech.petcare.common.thread.ThreadPoolManager;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class CoreThreadPoolConfig {

    @Bean(name = "coreThreadPool")
    public ThreadPoolExecutor coreThreadPool(MeterRegistry meterRegistry) {
        ThreadPoolExecutor theadPool = ThreadPoolManager.createTheadPool("core-service");
        ExecutorServiceMetrics.monitor(meterRegistry, theadPool, "core-service");
        // 预热线程
        theadPool.prestartAllCoreThreads();
        return theadPool;
    }

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags("application", "pet-care-core");
    }
}