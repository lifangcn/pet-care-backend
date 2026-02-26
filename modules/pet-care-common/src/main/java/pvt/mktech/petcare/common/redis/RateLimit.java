package pvt.mktech.petcare.common.redis;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @description}: 限流注解
 * {@code @date}: 2026/02/26
 *
 * @author Michael Li
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流key前缀
     */
    String key() default "";

    /**
     * 时间窗口（秒）
     */
    int interval() default 60;

    /**
     * 时间窗口内最大请求次数
     */
    int maxRequests() default 10;

    /**
     * 限流类型
     */
    LimitType limitType() default LimitType.DEFAULT;

    enum LimitType {
        /**
         * 默认策略（根据方法名+key）
         */
        DEFAULT,
        /**
         * 根据用户ID限流
         */
        USER,
        /**
         * 根据IP地址限流
         */
        IP,
        /**
         * 根据自定义key限流（需配合 SpEL 表达式使用）
         */
        CUSTOM
    }
}
