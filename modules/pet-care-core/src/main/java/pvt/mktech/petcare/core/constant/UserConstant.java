package pvt.mktech.petcare.core.constant;

/**
 * {@code @description}: 常量类
 * {@code @date}: 2025/12/2 11:47
 *
 * @author Michael
 */
public class UserConstant {

    public static final String USER_DEFAULT_NAME_PREFIX = "user_";
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 120L;

    // Token
    public static final Long ACCESS_TOKEN_TTL = 86400L;
    public static final String REFRESH_TOKEN_KEY = "login:refresh:token:";
    public static final Long REFRESH_TOKEN_TTL = 604800L;
}