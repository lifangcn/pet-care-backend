package pvt.mktech.petcare.core.handler;

import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import pvt.mktech.petcare.common.context.UserContext;
import pvt.mktech.petcare.core.dto.message.ReminderExecutionMessageDto;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 提醒 WebSocket 处理器
 * 管理 WebSocket 连接和消息发送
 */
@Component
public class ReminderWebSocketHandler extends TextWebSocketHandler {

    // 存储用户ID和WebSocket会话的映射
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    /**
     * 连接建立后调用
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从 session 的 attributes 中获取用户ID

        Long userId = (Long) session.getAttributes().get("userId");

        if (userId != null) {
            // 如果用户已有连接，关闭旧连接
            WebSocketSession oldSession = userSessions.get(userId);
            if (oldSession != null && oldSession.isOpen()) {
                oldSession.close();
            }

            // 存储新连接
            userSessions.put(userId, session);
            System.out.println("用户 " + userId + " WebSocket 连接已建立，当前在线用户数: " + userSessions.size());
        } else {
            System.out.println("警告: WebSocket 连接建立但 userId 为空，attributes: " + session.getAttributes());
        }
    }

    /**
     * 收到客户端消息时调用
     * 前端可能发送心跳消息保持连接
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 可以处理客户端发送的消息，如心跳检测
        String payload = message.getPayload();
        System.out.println("收到消息: " + payload);

        // 如果是心跳消息，可以回复
        if ("ping".equals(payload)) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    /**
     * 连接关闭后调用
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");

        if (userId != null) {
            userSessions.remove(userId);
            System.out.println("用户 " + userId + " WebSocket 连接已关闭");
        }
    }

    /**
     * 向指定用户发送提醒消息
     */
    public void sendReminderToUser(Long userId, ReminderExecutionMessageDto message) throws IOException {
        WebSocketSession session = userSessions.get(userId);

        if (session != null && session.isOpen()) {
            String jsonMessage = JSONUtil.toJsonStr(message);
            session.sendMessage(new TextMessage(jsonMessage));
            System.out.println("成功向用户 " + userId + " 发送提醒消息");
        } else {
            System.out.println("用户 " + userId + " 的 WebSocket 连接不存在或已关闭，当前在线用户: " + userSessions.keySet());
        }
    }

    /**
     * 获取所有在线用户ID
     */
    public java.util.Set<Long> getOnlineUsers() {
        return userSessions.keySet();
    }
}