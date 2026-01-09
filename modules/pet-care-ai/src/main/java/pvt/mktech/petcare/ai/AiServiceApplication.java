package pvt.mktech.petcare.ai;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * {@code @description}: 智能助手服务启动类
 * {@code @date}: 2025/12/30 14:15
 *
 * @author Michael
 */
@SpringBootApplication
@MapperScan("pvt.mktech.petcare.ai.mapper")
@EnableDubbo
public class AiServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
