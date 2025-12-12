package pvt.mktech.petcare.common.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 基于 Redisson 实现的分布式锁工具类，用于在分布式环境下对共享资源进行互斥访问控制。
 */
@Slf4j
@RequiredArgsConstructor
public class DistributedLock {

    private final RedissonClient redissonClient;

    /**
     * 在分布式锁保护下执行指定业务逻辑（带返回值）
     *
     * @param lockKey   锁的键名，用于标识一个唯一的锁
     * @param waitTime  获取锁的最大等待时间，超过此时间未获取到锁则抛出异常
     * @param leaseTime 锁的自动释放时间，防止死锁
     * @param timeUnit  时间单位，适用于 waitTime 和 leaseTime
     * @param supplier  业务逻辑供应者，其 get() 方法将在获得锁后执行
     * @param <T>       返回值泛型类型
     * @return 业务逻辑执行结果
     * @throws RuntimeException 如果无法获取锁或执行过程中发生异常
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (!acquired) {
                throw new RuntimeException("获取分布式锁失败: " + lockKey);
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取分布式锁被中断: " + lockKey, e);
        } catch (Exception e) {
            log.error("执行分布式锁业务逻辑异常: {}", lockKey, e);
            throw e;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 在分布式锁保护下执行指定业务逻辑（无返回值）
     *
     * @param lockKey   锁的键名，用于标识一个唯一的锁
     * @param waitTime  获取锁的最大等待时间，超过此时间未获取到锁则抛出异常
     * @param leaseTime 锁的自动释放时间，防止死锁
     * @param timeUnit  时间单位，适用于 waitTime 和 leaseTime
     * @param runnable  要执行的业务逻辑
     * @throws RuntimeException 如果无法获取锁或执行过程中发生异常
     */
    public void executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Runnable runnable) {
        executeWithLock(lockKey, waitTime, leaseTime, timeUnit, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 使用默认等待时间为0的分布式锁执行业务逻辑（带返回值）
     *
     * @param lockKey   锁的键名，用于标识一个唯一的锁
     * @param leaseTime 锁的自动释放时间，防止死锁
     * @param timeUnit  时间单位，适用于 leaseTime
     * @param supplier  业务逻辑供应者，其 get() 方法将在获得锁后执行
     * @param <T>       返回值泛型类型
     * @return 业务逻辑执行结果
     * @throws RuntimeException 如果无法获取锁或执行过程中发生异常
     */
    public <T> T executeWithLock(String lockKey, long leaseTime, TimeUnit timeUnit, Supplier<T> supplier) {
        return executeWithLock(lockKey, 0, leaseTime, timeUnit, supplier);
    }

    /**
     * 使用默认等待时间为0的分布式锁执行业务逻辑（无返回值）
     *
     * @param lockKey   锁的键名，用于标识一个唯一的锁
     * @param leaseTime 锁的自动释放时间，防止死锁
     * @param timeUnit  时间单位，适用于 leaseTime
     * @param runnable  要执行的业务逻辑
     * @throws RuntimeException 如果无法获取锁或执行过程中发生异常
     */
    public void executeWithLock(String lockKey, long leaseTime, TimeUnit timeUnit, Runnable runnable) {
        executeWithLock(lockKey, 0, leaseTime, timeUnit, runnable);
    }

    /**
     * 使用默认等待时间为0、租期为30秒的分布式锁执行业务逻辑（带返回值）
     *
     * @param lockKey  锁的键名，用于标识一个唯一的锁
     * @param supplier 业务逻辑供应者，其 get() 方法将在获得锁后执行
     * @param <T>      返回值泛型类型
     * @return 业务逻辑执行结果
     * @throws RuntimeException 如果无法获取锁或执行过程中发生异常
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        return executeWithLock(lockKey, 0, 30, TimeUnit.SECONDS, supplier);
    }
}

