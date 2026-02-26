package pvt.mktech.petcare.common.redis;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RedissonClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.web.UserContext;

import java.lang.reflect.Method;

/**
 * {@code @description}: 限流切面
 * {@code @date}: 2026/02/26
 *
 * @author Michael Li
 */
@Slf4j
@Aspect
public record RateLimitAspect(RedissonClient redissonClient) {

    private static final String IP_SEPARATOR = ".";
    private static final String UNKNOWN = "unknown";
    private static final String IP_SEPARATOR_REGEX = "\\.";

    @Around(value = "@annotation(rateLimit)", argNames = "joinPoint,rateLimit")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 1. 构建限流key
        String limitKey = buildLimitKey(joinPoint, rateLimit);

        // 2. 计算速率：每秒生成的令牌数 = maxRequests / interval
        long rate = Math.max(1L, rateLimit.maxRequests() / rateLimit.interval());

        // 3. 尝试获取令牌
        RateLimitUtil rateLimitUtil = new RateLimitUtil(redissonClient);
        boolean acquired = rateLimitUtil.tryAcquire(limitKey, 1, rate, rateLimit.interval());

        if (!acquired) {
            log.warn("限流触发，key: {}, rate: {}/{}s", limitKey, rateLimit.maxRequests(), rateLimit.interval());
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }

        return joinPoint.proceed();
    }

    /**
     * 构建限流key
     */
    private String buildLimitKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();

        StringBuilder keyBuilder = new StringBuilder("rate_limit:")
                .append(className).append(":").append(methodName);

        // 根据限流类型添加后缀
        switch (rateLimit.limitType()) {
            case USER -> {
                Long userId = UserContext.getUserId();
                if (userId != null) {
                    keyBuilder.append(":user:").append(userId);
                }
            }
            case IP -> {
                String ip = getClientIp();
                if (StrUtil.isNotBlank(ip)) {
                    // 对IP进行脱敏处理，保留前两段
                    String maskedIp = maskIp(ip);
                    keyBuilder.append(":ip:").append(maskedIp);
                }
            }
            case CUSTOM -> {
                if (StrUtil.isNotBlank(rateLimit.key())) {
                    keyBuilder.append(":").append(rateLimit.key());
                }
            }
            case DEFAULT -> {
                // 使用自定义key或方法签名
                if (StrUtil.isNotBlank(rateLimit.key())) {
                    keyBuilder.append(":").append(rateLimit.key());
                }
            }
        }

        return keyBuilder.toString();
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return UNKNOWN;
        }

        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多个IP的情况，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * IP脱敏处理，保留前两段
     * 例如：192.168.1.1 -> 192.168.*
     */
    private String maskIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return UNKNOWN;
        }
        String[] parts = ip.split(IP_SEPARATOR_REGEX);
        if (parts.length >= 2) {
            return parts[0] + IP_SEPARATOR + parts[1];
        }
        return ip;
    }
}
