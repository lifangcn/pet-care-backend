package pvt.mktech.petcare.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * {@code @description}: 聊天记忆配置
 * {@code @date}: 2026-03-02
 * @author Michael
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.chat.memory")
public class ChatMemoryProperties {

    /**
     * Redis 会话记忆配置
     */
    @NestedConfigurationProperty
    private Redis redis = new Redis();

    /**
     * 语义记忆配置
     */
    @NestedConfigurationProperty
    private Semantic semantic = new Semantic();

    /**
     * 会话历史配置
     */
    @NestedConfigurationProperty
    private History history = new History();

    /**
     * Redis 配置
     */
    @Data
    public static class Redis {
        private String host = "localhost";
        private Integer port = 6379;
        private String password = "!QAZ2wsx";
        private Integer timeout = 5000;
        private Integer maxMessages = 10;
    }

    /**
     * 语义记忆配置
     */
    @Data
    public static class Semantic {
        private boolean enabled = true;
        private int topK = 3;
        private double minScore = 0.75;
        private int historyDays = 30;
    }

    /**
     * 会话历史配置
     */
    @Data
    public static class History {
        private boolean enabled = true;
        private String indexName = "chat_history";
        private int retentionDays = 90;
    }
}
