package pvt.mktech.petcare.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pvt.mktech.petcare.common.thread.ThreadPoolManager;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * {@code @description} 异步线程池配置
 * {@code @date} 2026-01-25
 * {@code @author} Michael
 */
@Slf4j
@Configuration
public class AsyncConfig implements WebMvcConfigurer {

    @Resource
    private MeterRegistry meterRegistry;

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        ThreadPoolExecutor threadPoolExecutor = ThreadPoolManager.createThreadPool("mvc-async");
        ExecutorServiceMetrics.monitor(meterRegistry, threadPoolExecutor, "mvc-async");
        // 预热线程
        threadPoolExecutor.prestartAllCoreThreads();
        configurer.setTaskExecutor(new TaskExecutorAdapter(threadPoolExecutor));
        configurer.setDefaultTimeout(30000);
        log.info("异步线程池配置成功");
    }
}
