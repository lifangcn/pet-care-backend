package pvt.mktech.petcare.common.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code @description}: 创建线程或线程池时请指定有意义的线程名称，方便出错时回溯
 * {@code @date}: 2025/12/1 12:56
 *
 * @author Michael
 */
public class CustomThreadFactory implements ThreadFactory {
    private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;
    private final boolean daemon;

    /**
     * 创建普通线程
     * @param poolName 线程池名称，用于生成线程名称前缀
     */
    public CustomThreadFactory(String poolName) {
        this(poolName, false);
    }

    /**
     * 构造函数，创建线程工厂
     * @param poolName 线程池名称，用于生成线程名称前缀
     * @param daemon 是否创建守护线程
     */
    public CustomThreadFactory(String poolName, boolean daemon) {
        this.daemon = daemon;
        this.group = Thread.currentThread().getThreadGroup();
        this.namePrefix = poolName + "-pool-" + POOL_NUMBER.getAndIncrement() + "-thread-";
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(group, r, namePrefix + threadNumber, 0);
        thread.setDaemon(daemon);
        if (thread.getPriority() != Thread.NORM_PRIORITY) {
            thread.setPriority(Thread.NORM_PRIORITY);
        }
        return thread;
    }
}
