package pvt.mktech.petcare.common.redis;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.exception.SystemException;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class RedissonLockUtil {

    private final RedissonClient redissonClient;

    public RedissonLockUtil(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 可重入锁（公平锁）
     */
    public RLock getFairLock(String lockKey) {
        return redissonClient.getFairLock(lockKey);
    }

    /**
     * 可重入锁（非公平锁）
     */
    public RLock getLock(String lockKey) {
        return redissonClient.getLock(lockKey);
    }

    /**
     * 读锁
     */
    public RLock getReadLock(String lockKey) {
        return redissonClient.getReadWriteLock(lockKey).readLock();
    }

    /**
     * 写锁
     */
    public RLock getWriteLock(String lockKey) {
        return redissonClient.getReadWriteLock(lockKey).writeLock();
    }

    /**
     * 尝试获取锁（立即返回）
     *
     * @param lockKey   锁key
     * @param waitTime  等待时间（秒）
     * @param leaseTime 持有时间（秒，自动释放）
     * @param unit      时间单位
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取锁失败，lockKey: {}", lockKey, e);
            return false;
        }
    }

    /**
     * 尝试获取锁（默认配置）
     */
    public boolean tryLock(String lockKey) {
        return tryLock(lockKey, 3, 30, TimeUnit.SECONDS);
    }

    /**
     * 释放锁
     */
    public void unlock(String lockKey) {
        RLock lock = getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 释放指定锁对象
     */
    public void unlock(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 执行带锁的方法（自动获取和释放锁）
     *
     * @param lockKey   锁key
     * @param supplier  要执行的方法
     * @param waitTime  等待时间（秒）
     * @param leaseTime 持有时间（秒）
     * @return 执行结果
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier,
                                 long waitTime, long leaseTime) {
        RLock lock = getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (locked) {
                return supplier.get();
            } else {
                throw new SystemException(ErrorCode.LOCK_ACQUIRE_FAILED,
                        ErrorCode.LOCK_ACQUIRE_FAILED.getMessage() + ": " + lockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SystemException(ErrorCode.LOCK_INTERRUPTED,
                    ErrorCode.LOCK_INTERRUPTED.getMessage() + ": " + lockKey, e);
        } finally {
            if (locked) {
                unlock(lock);
            }
        }
    }

    /**
     * 执行带锁的方法（默认配置）
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        return executeWithLock(lockKey, supplier, 3, 30);
    }

    /**
     * 执行带锁的方法（无返回值）
     */
    public void executeWithLock(String lockKey, Runnable runnable) {
        executeWithLock(lockKey, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 执行带锁的方法（快速失败）
     */
    public boolean tryExecuteWithLock(String lockKey, Runnable runnable) {
        RLock lock = getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(0, 30, TimeUnit.SECONDS);
            if (locked) {
                runnable.run();
                return true;
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (locked) {
                unlock(lock);
            }
        }
    }

    /**
     * 检查锁是否被当前线程持有
     */
    public boolean isLockedByCurrentThread(String lockKey) {
        RLock lock = getLock(lockKey);
        return lock.isHeldByCurrentThread();
    }

    /**
     * 获取锁的剩余时间
     */
    public long getLockRemainTime(String lockKey) {
        RLock lock = getLock(lockKey);
        try {
            return lock.remainTimeToLive();
        } catch (Exception e) {
            log.error("获取锁剩余时间失败", e);
            return -1;
        }
    }
}