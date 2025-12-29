package pvt.mktech.petcare.core.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import pvt.mktech.petcare.common.util.JwtUtil;

import java.util.Map;

/**
 * WebSocket 认证拦截器
 * 在建立连接前验证用户 token
 */
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    public WebSocketAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * 握手前拦截，验证 token
     */
    @Override
    public boolean beforeHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();

            // 从查询参数获取 token
            String token = httpRequest.getParameter("token");

            if (token == null || token.isEmpty()) {
                // token 为空，拒绝连接
                response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return false;
            }
            // 验证 token 并获取用户信息
            Long userId = validateTokenAndGetUserId(token);

            if (userId == null) {
                // token 无效，拒绝连接
                response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return false;
            }

            // 将用户ID存储到 attributes 中，供后续使用
            attributes.put("userId", userId);
            attributes.put("token", token);

            return true;
        }

        return false;
    }

    /**
     * 握手后处理
     */
    @Override
    public void afterHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            Exception exception) {
        // 握手后的处理逻辑（可选）
    }

    /**
     * 验证 token 并获取用户ID
     */
    private Long validateTokenAndGetUserId(String token) {
        try {
            if (!jwtUtil.validateToken(token)) {
                return null;
            }
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }
}