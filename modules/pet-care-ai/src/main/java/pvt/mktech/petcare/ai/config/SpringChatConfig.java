package pvt.mktech.petcare.ai.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.memory.redis.LettuceRedisChatMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import pvt.mktech.petcare.ai.tool.ReminderTool;

import java.nio.charset.StandardCharsets;

/**
 * {@code @description}: Spring AI 配置类
 * 职责：
 * 1. 初始化向量数据库（Milvus VectorStore）
 * 2. 加载知识库文档（PDF/MD）
 * 3. 文档切分和向量化
 * 4. 存储到 Milvus 向量数据库
 * {@code @date}: 2025/12/30 14:46
 *
 * @author Michael
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({ChatMemoryProperties.class})
public class SpringChatConfig {

    private final ReminderTool reminderTool; // 本地 Tools

    @Bean
    public ChatClient chatClient(DashScopeChatModel chatModel,
                                 ChatMemory chatMemory,
                                 ToolCallbackProvider toolCallbackProvider) {
        ToolCallback[] localTools = ToolCallbacks.from(reminderTool); // 本地 Tools
        return ChatClient.builder(chatModel)
                .defaultToolCallbacks(localTools)
                .defaultToolCallbacks(toolCallbackProvider.getToolCallbacks())
                .defaultSystem(loadSystemPrompt())
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        PromptChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository, ChatMemoryProperties properties) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(properties.getMaxMessages())
                .build();
    }

    @Bean
    public ChatMemoryRepository redisChatMemoryRepository(ChatMemoryProperties properties) {
        return LettuceRedisChatMemoryRepository.builder()
                .host(properties.getHost())
                .port(properties.getPort())
                .password(properties.getPassword())
                .timeout(properties.getTimeout())
                .build();
    }


    // ollama 太慢了
    /*@Bean
    public ChatClient chatClient(OllamaChatModel chatModel, RedisChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultSystem("你是一个具备10年架构经验的Java程序员导师")
                .defaultAdvisors(
//                        new SimpleLoggerAdvisor(),
                        PromptChatMemoryAdvisor.builder(chatMemory).build()
                ).build();
    }*/

    /**
     * 加载系统提示词
     * 系统提示词的作用：
     * 1. 定义 AI 的角色（志愿填报顾问）
     * 2. 定义 AI 的行为规范
     * 3. 定义 AI 的能力范围
     * 4. 定义输出格式要求
     */
    private String loadSystemPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource("system.txt");
            String prompt = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            log.info("加载系统提示词：{}", prompt);
            return prompt;
        } catch (Exception e) {
            return "你是宠物关怀提供的专业的服务咨询顾问";
        }
    }
}
