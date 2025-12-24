package pvt.mktech.petcare.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pvt.mktech.petcare.common.thread.ThreadPoolManager;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class CoreThreadPoolConfig {

    @Bean(name = "coreThreadPool")
    public ThreadPoolExecutor coreThreadPool() {
        ThreadPoolExecutor theadPool = ThreadPoolManager.createTheadPool("core-service");
        // 预热线程
        theadPool.prestartAllCoreThreads();
        return theadPool;
    }
}