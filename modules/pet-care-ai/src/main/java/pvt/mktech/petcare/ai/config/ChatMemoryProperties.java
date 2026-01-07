package pvt.mktech.petcare.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "spring.ai.chat.memory.redis")
public class ChatMemoryProperties {
    private String host = "localhost";
    private Integer port = 6379;
    private String password = "!QAZ2wsx";
    private Integer timeout = 5000;
    private Integer maxMessages = 10;
}

