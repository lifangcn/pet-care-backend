package pvt.mktech.petcare.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.common.dto.response.ResultCode;
import pvt.mktech.petcare.gateway.util.RateLimiterUtil;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 网关限流过滤器
 * 基于 Redis + Redisson 实现分布式限流
 * 支持按路径模式配置不同的限流规则
 */
@Slf4j
@Order(-100)
@Component
public class RateLimitGatewayFilterFactory extends AbstractGatewayFilterFactory<RateLimitGatewayFilterFactory.Config> {

    private static final String RATE_LIMIT_KEY_PREFIX = "rate:limit:";
    private static final String UNKNOWN_IP = "unknown";
    private static final int DEFAULT_RATE_LIMIT = 100;

    private final RateLimiterUtil rateLimiterUtil;
    private final ObjectMapper objectMapper;
    private final PathMatcher pathMatcher;

    /**
     * API限流配置：路径模式 -> 每秒请求数
     * 使用 LinkedHashMap 保持顺序，优先匹配更具体的路径
     */
    private static final Map<String, Integer> RATE_LIMIT_CONFIG = new LinkedHashMap<>();

    static {
        // 登录接口：5次/秒
        RATE_LIMIT_CONFIG.put("/api/auth/login", 5);
        // 验证码接口：3次/秒
        RATE_LIMIT_CONFIG.put("/api/auth/code", 3);
        // 其他API：100次/秒
        RATE_LIMIT_CONFIG.put("/api/**", DEFAULT_RATE_LIMIT);
    }

    public RateLimitGatewayFilterFactory(RateLimiterUtil rateLimiterUtil, ObjectMapper objectMapper) {
        super(Config.class);
        this.rateLimiterUtil = rateLimiterUtil;
        this.objectMapper = objectMapper;
        this.pathMatcher = new AntPathMatcher();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            String ip = getClientIp(request);

            // 跳过限流的路径
            if (shouldSkipRateLimit(path, config)) {
                return chain.filter(exchange);
            }

            // 获取限流配置（优先使用配置中的，否则使用默认配置）
            int rateLimit = config.getRateLimit() != null
                    ? config.getRateLimit() : getRateLimitForPath(path);

            // 构造限流key：IP + 路径
            String rateLimitKey = buildRateLimitKey(ip, path);

            // 尝试获取令牌
            if (!rateLimiterUtil.tryAcquire(rateLimitKey, rateLimit)) {
                log.warn("请求被限流: IP={}, 路径={}, 限流速率={}/秒", ip, path, rateLimit);
                return buildTooManyRequestsResponse(exchange.getResponse());
            }

            return chain.filter(exchange);
        };
    }

    /**
     * 判断是否应该跳过限流
     */
    private boolean shouldSkipRateLimit(String path, Config config) {
        if (config.getExcludePaths() != null && !config.getExcludePaths().isEmpty()) {
            return config.getExcludePaths().stream()
                    .anyMatch(pattern -> pathMatcher.match(pattern, path));
        }
        return false;
    }

    /**
     * 根据路径匹配限流规则
     * 优先匹配更具体的路径模式
     */
    private int getRateLimitForPath(String path) {
        return RATE_LIMIT_CONFIG.entrySet().stream()
                .filter(entry -> pathMatcher.match(entry.getKey(), path))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(DEFAULT_RATE_LIMIT);
    }

    /**
     * 获取客户端IP地址
     * 优先从 X-Forwarded-For 获取（支持代理场景）
     */
    private String getClientIp(ServerHttpRequest request) {
        // 1. 从 X-Forwarded-For 获取（代理场景）
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            // 取第一个IP（可能是多个IP，用逗号分隔）
            String[] ips = xForwardedFor.split(",");
            if (ips.length > 0) {
                String ip = ips[0].trim();
                if (StringUtils.hasText(ip)) {
                    return ip;
                }
            }
        }

        // 2. 从 X-Real-IP 获取（Nginx代理场景）
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp.trim();
        }

        // 3. 从 RemoteAddress 获取
        if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }

        return UNKNOWN_IP;
    }

    /**
     * 构造限流key
     */
    private String buildRateLimitKey(String ip, String path) {
        return RATE_LIMIT_KEY_PREFIX + ip + ":" + path;
    }

    /**
     * 构建429响应
     */
    private Mono<Void> buildTooManyRequestsResponse(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Result<String> result = Result.error(
                ResultCode.TOO_MANY_REQUESTS.getCode(),
                ResultCode.TOO_MANY_REQUESTS.getMessage()
        );

        try {
            byte[] bytes = objectMapper.writeValueAsString(result)
                    .getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败", e);
            return response.setComplete();
        }
    }

    /**
     * 过滤器配置类
     */
    @Data
    public static class Config {
        /**
         * 限流速率（每秒请求数）
         * 如果配置了此值，将覆盖路径匹配的限流规则
         */
        private Integer rateLimit;

        /**
         * 排除限流的路径模式列表
         * 匹配的路径将不进行限流
         */
        private java.util.List<String> excludePaths;
    }
}
