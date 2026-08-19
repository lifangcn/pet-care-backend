package pvt.mktech.petcare;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import pvt.mktech.petcare.observability.config.ObservabilityAutoConfiguration;

import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;

/**
 * {@code @description}: 智能助手服务启动类
 * {@code @date}: 2025/12/30 14:15
 *
 * @author Michael
 */
@SpringBootApplication(exclude = {
    // 排除 OpenAI 自动配置（手动创建 DeepSeek ChatModel）
    OpenAiChatAutoConfiguration.class,
})
@MapperScan({
    "pvt.mktech.petcare.knowledge.mapper",
    "pvt.mktech.petcare.sync.mapper"
})
@Import(ObservabilityAutoConfiguration.class)
public class AiServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
