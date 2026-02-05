package pvt.mktech.petcare.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * {@code @description}: 智能助手服务启动类
 * {@code @date}: 2025/12/30 14:15
 *
 * @author Michael
 */
@SpringBootApplication(scanBasePackages = {
    "pvt.mktech.petcare.knowledge",
    "pvt.mktech.petcare.infrastructure",
    "pvt.mktech.petcare.shared",
    "pvt.mktech.petcare.chat",
    "pvt.mktech.petcare.sync",
})
@MapperScan({
    "pvt.mktech.petcare.knowledge.mapper",
    "pvt.mktech.petcare.sync.mapper"
})
public class AiServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
