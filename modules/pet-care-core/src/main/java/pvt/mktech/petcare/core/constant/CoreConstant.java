package pvt.mktech.petcare.core.constant;

/**
 * {@code @description}: 常量类
 * {@code @date}: 2025/12/2 11:47
 *
 * @author Michael
 */
public class CoreConstant {

    public static final String USER_DEFAULT_NAME_PREFIX = "user_";
    public static final String LOGIN_CODE_KEY = "core:auth:code:";
    public static final Long LOGIN_CODE_TTL = 120L;

    // Token
    public static final Long ACCESS_TOKEN_TTL = 86400L;
    public static final String REFRESH_TOKEN_KEY = "core:auth:refresh_token:";
    public static final Long REFRESH_TOKEN_TTL = 604800L;

    // 消息队列相关(组装消息体逻辑：Topic: core-reminder-delay, Tag: pending|send, Key: executionId, Body: ExecutionDto)
    public static final String CORE_REMINDER_DELAY_TOPIC_PENDING = "CORE_REMINDER_PENDING";
    public static final String CORE_REMINDER_DELAY_TOPIC_SEND = "CORE_REMINDER_SEND";
    public static final String CORE_REMINDER_PRODUCER = "core-reminder-producer";
    public static final String CORE_REMINDER_PENDING_CONSUMER = "core-reminder-pending-consumer";
    public static final String CORE_REMINDER_SEND_CONSUMER = "core-reminder-send-consumer";
    // Redis 对于延迟消费的信息，进行缓存
    public static final String CORE_REMINDER_SEND_QUEUE_KEY = "core:reminder:send_queue";
    // 用户签到
    public static final String CORE_USER_CHECKIN_KEY = "core:user:checkin:";

}