package pvt.mktech.petcare.gateway.filter;

import lombok.Data;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

/**
 * SSE (Server-Sent Events) 网关过滤器
 * 用于配置 SSE 流式响应：禁用缓冲、设置响应头
 */
@Component
public class SseGatewayFilterFactory extends AbstractGatewayFilterFactory<SseGatewayFilterFactory.Config> {

    public SseGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpResponse response = exchange.getResponse();
            HttpHeaders headers = response.getHeaders();

            // 禁用 Nginx 缓冲（关键：确保数据实时传输）
            headers.add("X-Accel-Buffering", "no");

            // 设置 SSE 响应头（所有经过此过滤器的路由都是 SSE 接口）
            headers.set(HttpHeaders.CONTENT_TYPE, "text/event-stream;charset=UTF-8");

            // 设置缓存控制
            headers.set(HttpHeaders.CACHE_CONTROL, "no-cache");
            headers.set(HttpHeaders.CONNECTION, "keep-alive");

            return chain.filter(exchange);
        };
    }

    @Data
    public static class Config {
        // 可扩展配置项
    }
}

