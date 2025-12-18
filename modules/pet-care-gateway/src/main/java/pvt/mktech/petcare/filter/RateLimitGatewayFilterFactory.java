package pvt.mktech.petcare.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.common.dto.response.ResultCode;
import pvt.mktech.petcare.common.util.RateLimiterUtil;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code @description}: TODO 限流过滤器
 * {@code @date}: 2025/12/15 10:41
 *
 * @author Michael
 */
@Order(-100)
@Component
public class RateLimitGatewayFilterFactory extends AbstractGatewayFilterFactory<RateLimitGatewayFilterFactory.Config> {
    private final RateLimiterUtil rateLimiterUtil;
    private final ObjectMapper objectMapper;
    private final PathMatcher pathMatcher = new AntPathMatcher();

    // API限流配置：路径模式 -> 每秒请求数
    private static final Map<String, Integer> RATE_LIMIT_CONFIG = new HashMap<>();

    static {
        RATE_LIMIT_CONFIG.put("/api/auth/login", 5);    // 登录接口：5次/秒
        RATE_LIMIT_CONFIG.put("/api/auth/code", 3);     // 注册接口：3次/秒
        RATE_LIMIT_CONFIG.put("/api/**", 100);          // 其他API：100次/秒
    }

    public RateLimitGatewayFilterFactory(RateLimiterUtil rateLimiterUtil, ObjectMapper objectMapper) {
        super(Config.class);
        this.rateLimiterUtil = rateLimiterUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            String ip = getClientIp(exchange.getRequest());

            // 1. 根据路径匹配限流规则
            int rateLimit = getRateLimitForPath(path);

            // 2. 构造限流key：IP + 路径
            String rateLimitKey = "rate:limit:" + ip + ":" + path;

            // 3. 尝试获取令牌
            if (!rateLimiterUtil.tryAcquire(rateLimitKey, rateLimit)) {
//                log.warn("请求被限流: IP={}, 路径={}", ip, path);
                return tooManyRequests(exchange.getResponse(), "请求过于频繁，请稍后再试");
            }

            return chain.filter(exchange);
        };
    }

    private int getRateLimitForPath(String path) {
        return RATE_LIMIT_CONFIG.entrySet().stream()
                .filter(entry -> pathMatcher.match(entry.getKey(), path))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(100); // 默认100次/秒
    }

    private String getClientIp(org.springframework.http.server.reactive.ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddress() != null ?
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    private Mono<Void> tooManyRequests(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Result<String> result = Result.error(ResultCode.FAILED, message);

        try {
            byte[] bytes = objectMapper.writeValueAsString(result)
                    .getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
//            log.error("JSON序列化失败", e);
            return response.setComplete();
        }
    }

    public static class Config {
        // 配置属性
    }
}
