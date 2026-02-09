package pvt.mktech.petcare.infrastructure.constant;

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

    // 微信登录
    public static final String WECHAT_LOGIN_TICKET_KEY = "core:auth:wechat:ticket:";
    public static final Long WECHAT_LOGIN_TTL = 300L;

    // 消息队列相关(组装消息体逻辑：Topic: CORE_REMINDER_PENDING/SEND, Tag: pending|send, Key: executionId, Body: ExecutionDto)
    public static final String CORE_REMINDER_PENDING_TOPIC = "core_reminder_pending";
    public static final String CORE_REMINDER_SEND_TOPIC = "core_reminder_send";
    public static final String CORE_REMINDER_PENDING_CONSUMER = "core_reminder_pending_consumer";
    public static final String CORE_REMINDER_SEND_CONSUMER = "core_reminder_send_consumer";
    // Redis 对于延迟消费的信息，进行缓存
    public static final String CORE_REMINDER_SEND_QUEUE_KEY = "core:reminder:send_queue";
    /** 用户行为 **/
    /**
     * 签到，13个月过期
     */
    public static final String CORE_USER_CHECK_IN_KEY = "core:user:check_in:";
    /**
     * 当日行为记录， Hash 包含 publish, comment, like, share
     */
    public static final String CORE_USER_ACTION_KEY = "core:user:action:";

    /** 积分系统 **/
    /**
     * 内容被点赞去重 Set，key格式: core:content:like:{contentId}:{date}
     */
    public static final String CORE_CONTENT_LIKE_KEY = "core:content:like:";
    /**
     * 内容被评论去重 Set，key格式: core:content:comment:{contentId}:{date}
     */
    public static final String CORE_CONTENT_COMMENT_KEY = "core:content:comment:";
    // 提醒扫描锁KEY
    public static final String REMINDER_SCAN_LOCK_KEY = "reminder:scan:lock";
    // 延迟队列扫描锁KEY
    public static final String DELAY_QUEUE_SCAN_LOCK_KEY = "reminder:delay_queue:scan:lock";
}