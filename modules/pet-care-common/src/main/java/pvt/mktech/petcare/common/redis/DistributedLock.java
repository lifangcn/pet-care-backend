package pvt.mktech.petcare.common.redis;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * {@code @description}: 自定义分布式锁注解
 * {@code @date}: 2026/1/29 15:00
 *
 * @author Michael Li
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * 锁名称
     */
    String lockKey();

    /**
     * 锁的过期时间，默认30秒
     */
    long leaseTime() default 30000;
    /**
     * 锁等待时间，默认10秒
     */
    long waitTime() default 10000;

    /**
     * 时间单位，默认毫秒
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
}
