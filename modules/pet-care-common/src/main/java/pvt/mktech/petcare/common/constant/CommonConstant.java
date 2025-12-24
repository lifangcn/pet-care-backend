package pvt.mktech.petcare.common.constant;

public class CommonConstant {
    // DistributedIdGenerator 相关
    public static final String ID_PREFIX = "id:generator:";
    public static final long BEGIN_TIMESTAMP = 1735689600L; // 开始时间戳2025-01-01 00:00:00
    public static final int COUNT_BITS = 32;

    // JWT相关
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USERNAME = "X-Username";
    // 限流
    public static final String RATE_LIMIT_KEY = "rate:limiter:api:";
//    public static final String REDIS_KEY_PREFIX = "petcare:";
//    public static final String REDIS_TOKEN_KEY = REDIS_KEY_PREFIX + "token:";
//    public static final String REDIS_LOCK_KEY = REDIS_KEY_PREFIX + "lock:";
}