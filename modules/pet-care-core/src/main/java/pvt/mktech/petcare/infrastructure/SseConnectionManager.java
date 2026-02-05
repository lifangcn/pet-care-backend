package pvt.mktech.petcare.infrastructure;

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
            userConnections.compute(userId, (k, v) -> (v != null && v.isEmpty()) ? null : v);
        }
    }

    public List<SseEmitter> getUserConnections(Long userId) {
        List<SseEmitter> list = userConnections.get(userId);
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    public void sendMessage(Long userId, String jsonString) {
        List<SseEmitter> emitters = getUserConnections(userId);
        if (emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("reminder").data(jsonString));
            } catch (IllegalStateException | IOException e) {
                // 连接已完成或失败，移除该连接
                removeConnection(userId, emitter);
            }
        }
    }
}
