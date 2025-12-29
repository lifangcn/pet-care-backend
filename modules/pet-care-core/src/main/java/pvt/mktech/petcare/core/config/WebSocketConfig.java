package pvt.mktech.petcare.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import pvt.mktech.petcare.core.handler.ReminderWebSocketHandler;
import pvt.mktech.petcare.core.interceptor.WebSocketAuthInterceptor;

/**
 * WebSocket 配置类
 * 配置原生 WebSocket 端点和处理器
 */
@Slf4j
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ReminderWebSocketHandler reminderWebSocketHandler;
    private final WebSocketAuthInterceptor authInterceptor;

    public WebSocketConfig(ReminderWebSocketHandler reminderWebSocketHandler,
                           WebSocketAuthInterceptor authInterceptor) {
        this.reminderWebSocketHandler = reminderWebSocketHandler;
        this.authInterceptor = authInterceptor;
    }

    /**
     * 注册 WebSocket 处理器
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册 /ws/reminders 端点
        // 添加认证拦截器
        // 允许跨域
        registry.addHandler(reminderWebSocketHandler, "/ws/reminders")
                .addInterceptors(authInterceptor)
                .setAllowedOriginPatterns("*");
    }
}