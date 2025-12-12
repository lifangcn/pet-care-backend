package pvt.mktech.petcare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pvt.mktech.petcare.common.thread.ThreadPoolManager;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class UserServiceThreadPoolConfig {

    @Bean(name = "userServiceThreadPool")
    public ThreadPoolExecutor userServiceThreadPool() {
        return ThreadPoolManager.createTheadPool("core-service");
    }
}