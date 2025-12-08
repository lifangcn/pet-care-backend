package pvt.mktech.petcare.common.redis;

import lombok.extern.slf4j.Slf4j;

/**
 * {@code @description}:
 * {@code @date}: 2025/11/28 14:09
 *
 * @author Michael
 */
//@Slf4j
//@Service
//@RequiredArgsConstructor
public class DistributedLockService {
/*
    private final RedissonClient redissonClient;

    *//**
     * 尝试获取锁并执行操作（自动释放）
     *//*
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime,
                                 TimeUnit timeUnit, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁
            boolean locked = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (!locked) {
                throw new DistributedLockException("获取分布式锁失败: " + lockKey);
            }

            try {
                // 执行业务操作
                return supplier.get();
            } finally {
                // 释放锁
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DistributedLockException("获取锁时被中断: " + lockKey, e);
        } catch (Exception e) {
            throw new DistributedLockException("分布式锁操作异常: " + lockKey, e);
        }
    }

    *//**
     * 尝试获取锁并执行操作（无返回值）
     *//*
    public void executeWithLock(String lockKey, long waitTime, long leaseTime,
                                TimeUnit timeUnit, Runnable runnable) {
        executeWithLock(lockKey, waitTime, leaseTime, timeUnit, () -> {
            runnable.run();
            return null;
        });
    }

    *//**
     * 快速获取锁（默认超时时间）
     *//*
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        return executeWithLock(lockKey, 3, 30, TimeUnit.SECONDS, supplier);
    }

    *//**
     * 获取公平锁
     *//*
    public <T> T executeWithFairLock(String lockKey, long waitTime, long leaseTime,
                                     TimeUnit timeUnit, Supplier<T> supplier) {
        RLock lock = redissonClient.getFairLock(lockKey);

        try {
            boolean locked = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (!locked) {
                throw new DistributedLockException("获取公平锁失败: " + lockKey);
            }

            try {
                return supplier.get();
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DistributedLockException("获取公平锁时被中断: " + lockKey, e);
        }
    }

    *//**
     * 获取读写锁（写锁）
     *//*
    public <T> T executeWithWriteLock(String lockKey, long waitTime, long leaseTime,
                                      TimeUnit timeUnit, Supplier<T> supplier) {
        RLock lock = redissonClient.getReadWriteLock(lockKey).writeLock();

        try {
            boolean locked = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (!locked) {
                throw new DistributedLockException("获取写锁失败: " + lockKey);
            }

            try {
                return supplier.get();
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DistributedLockException("获取写锁时被中断: " + lockKey, e);
        }
    }

    *//**
     * 获取读写锁（读锁）
     *//*
    public <T> T executeWithReadLock(String lockKey, long waitTime, long leaseTime,
                                     TimeUnit timeUnit, Supplier<T> supplier) {
        RLock lock = redissonClient.getReadWriteLock(lockKey).readLock();

        try {
            boolean locked = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (!locked) {
                throw new DistributedLockException("获取读锁失败: " + lockKey);
            }

            try {
                return supplier.get();
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DistributedLockException("获取读锁时被中断: " + lockKey, e);
        }
    }

    *//**
     * 手动获取锁（需要手动释放）
     *//*
    public RLock acquireLock(String lockKey, long waitTime, long leaseTime,
                             TimeUnit timeUnit) throws InterruptedException {
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = lock.tryLock(waitTime, leaseTime, timeUnit);
        return locked ? lock : null;
    }

    *//**
     * 分布式锁异常
     *//*
    public static class DistributedLockException extends RuntimeException {
        public DistributedLockException(String message) {
            super(message);
        }

        public DistributedLockException(String message, Throwable cause) {
            super(message, cause);
        }
    }*/
}
