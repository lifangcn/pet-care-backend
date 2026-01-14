package pvt.mktech.petcare.ai.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pvt.mktech.petcare.common.thread.ThreadPoolManager;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AiThreadPoolConfig {

    @Bean(name = "aiThreadPool")
    public ThreadPoolExecutor customThreadPool(MeterRegistry meterRegistry) {
        ThreadPoolExecutor theadPool = ThreadPoolManager.createTheadPool("ai-service");
        ExecutorServiceMetrics.monitor(meterRegistry, theadPool, "ai-service");
        // 预热线程
        theadPool.prestartAllCoreThreads();
        return theadPool;
    }

//    @Bean
//    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
//        return registry -> registry.config().commonTags("application", "pet-care-ai");
//    }
}