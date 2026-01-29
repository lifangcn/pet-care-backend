package pvt.mktech.petcare.common.redis;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.exception.SystemException;

import java.util.concurrent.TimeUnit;

/**
 * {@code @description}: 自定义分布式锁 切面
 * {@code @date}: 2026/1/29 15:00
 *
 * @author Michael Li
 */
@Slf4j
@Aspect
public record DistributedLockAspect(RedissonClient redissonClient) {

    @Around(value = "@annotation(distributedLock)", argNames = "joinPoint,distributedLock")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) {
        String lockKey = distributedLock.lockKey();
        long waitTime = distributedLock.waitTime();
        long leaseTime = distributedLock.leaseTime();
        TimeUnit timeUnit = distributedLock.timeUnit();
        // 获取方法的信息
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();

        // 获取锁
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (acquired) {
                return joinPoint.proceed();
            } else {
                throw new SystemException(ErrorCode.LOCK_ACQUIRE_FAILED, ErrorCode.LOCK_ACQUIRE_FAILED.getMessage() + ": " + lockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取锁过程中被中断，lockKey: {}, method: {}", lockKey, className + "." + methodName, e);
            throw new SystemException(ErrorCode.LOCK_INTERRUPTED, ErrorCode.LOCK_INTERRUPTED.getMessage() + ": " + lockKey, e);
        } catch (Throwable e) {
            log.error("执行业务方法失败，lockKey: {}, method: {}", lockKey, className + "." + methodName, e);
            throw new SystemException(ErrorCode.LOCK_OPERATION_FAILED, "执行方法失败: " + className + "." + methodName, e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
