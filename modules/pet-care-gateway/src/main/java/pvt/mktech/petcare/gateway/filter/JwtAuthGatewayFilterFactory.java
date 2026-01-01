package pvt.mktech.petcare.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.Data;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.server.ServerWebExchange;
import pvt.mktech.petcare.common.util.JwtUtil;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pvt.mktech.petcare.common.constant.CommonConstant.TOKEN_HEADER;
import static pvt.mktech.petcare.common.constant.CommonConstant.TOKEN_PREFIX;
import static pvt.mktech.petcare.common.constant.CommonConstant.HEADER_USER_ID;
import static pvt.mktech.petcare.common.constant.CommonConstant.HEADER_USERNAME;

@Order(-1)
@Component
public class JwtAuthGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthGatewayFilterFactory.Config> {

    private final JwtUtil jwtUtil;
    private final PathMatcher pathMatcher;
    private final ObjectMapper objectMapper;

    public JwtAuthGatewayFilterFactory(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
        this.pathMatcher = new AntPathMatcher();
        this.objectMapper = objectMapper;
    }

    @Data
    public static class Config {
        // 路由特定的白名单
        private List<String> whitelist = new ArrayList<>();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // 修改匹配逻辑
            if (config.getWhitelist() != null &&
                    config.getWhitelist().stream().anyMatch(pattern -> pathMatcher.match(pattern, path))) {
                return chain.filter(exchange);
            }

            // 2. 获取 Token（优先从 Header，其次从 URL 查询参数，用于 WebSocket）
            String token = request.getHeaders().getFirst(TOKEN_HEADER);
            if (token != null && token.startsWith(TOKEN_PREFIX)) {
                token = token.substring(TOKEN_PREFIX.length());
            } else if (request.getURI().getPath().startsWith("/ws")) {
                // 从 URL 查询参数获取 token（WebSocket 不支持自定义 Header）
                String query = request.getURI().getQuery();
                if (query != null) {
                    token = StrUtil.subAfter(query, "token=", false);
                }
            }

            if (token == null || token.isEmpty()) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            try {
                // 3. 验证 Token
                Claims claims = jwtUtil.parseToken(token);

                // 4. 解析用户信息并透传给下游服务
                Long userId = Long.parseLong(claims.getSubject());
                String username = claims.get(HEADER_USERNAME, String.class);

                ServerWebExchange serverWebExchange = exchange.mutate()
                        .request(builder -> builder
                                .header(HEADER_USER_ID, String.valueOf(userId))
                                .header(HEADER_USERNAME, username))
                        .build();

                return chain.filter(serverWebExchange);

            } catch (ExpiredJwtException e) {
                return unauthorizedResponse(exchange, "Token expired");
            } catch (Exception e) {
                return unauthorizedResponse(exchange, "Invalid token");
            }
        };
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 401);
        result.put("message", message);
        result.put("timestamp", System.currentTimeMillis());
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(result);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return response.setComplete();
        }
    }
}