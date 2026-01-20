package pvt.mktech.petcare.core.util;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@code @description}: SSE 连接管理器
 * {@code @date}: 2026/1/19 18:53
 *
 * @author Michael
 */
@Component
public class SseConnectionManager {
    private final Map<Long, List<SseEmitter>> userConnections = new ConcurrentHashMap<>();

    public void addConnection(Long userId, SseEmitter emitter) {
        userConnections.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
    }

    public void removeConnection(Long userId, SseEmitter emitter) {
        List<SseEmitter> connections = userConnections.get(userId);
        if (connections != null) {
            connections.remove(emitter);
            if (connections.isEmpty()) {
                userConnections.remove(userId);
            }
        }
    }

    public List<SseEmitter> getUserConnections(Long userId) {
        return userConnections.getOrDefault(userId, Collections.emptyList());
    }

    public void sendMessage(Long userId, String jsonString) {
        List<SseEmitter> emitters = getUserConnections(userId);
        if (emitters.isEmpty()) {
            return;
        }
        // 使用快照避免并发修改异常
        for (SseEmitter emitter : new CopyOnWriteArrayList<>(emitters)) {
            try {
                emitter.send(SseEmitter.event().name("reminder").data(jsonString));
            } catch (IOException e) {
                // 单个连接失败不影响其他连接，记录并移除
                removeConnection(userId, emitter);
            }
        }
    }
}
