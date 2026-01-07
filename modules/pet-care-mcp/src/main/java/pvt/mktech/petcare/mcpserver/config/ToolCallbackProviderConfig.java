package pvt.mktech.petcare.mcpserver.config;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pvt.mktech.petcare.mcpserver.tool.ReminderToolCallback;

/**
 * {@code @description}:
 * {@code @date}: 2026/1/6 16:41
 *
 * @author Michael
 */
@Configuration
public class ToolCallbackProviderConfig {

    @Bean
    public ToolCallbackProvider reminderToolCallbackProvider(ReminderToolCallback reminderToolCallback) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(reminderToolCallback)
                .build();
    }
}
