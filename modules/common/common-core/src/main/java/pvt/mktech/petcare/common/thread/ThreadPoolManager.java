package pvt.mktech.petcare.common.thread;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * {@code @description}: 创建线程池。遵循阿里巴巴规范：不使用Executors创建，而是通过ThreadPoolExecutor
 * {@code @date}: 2025/12/1 12:54
 *
 * @author Michael
 */
@Slf4j
public class ThreadPoolManager {

    /**
     * 创建自定义线程池方法
     *
     * @return 线程池对象
     */
    public static ThreadPoolExecutor createTheadPool(String BusinessPoolName) {
        // 必要参数：核心线程数，最大线程数，核心线程存活时间，单位
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int maxPoolSize = corePoolSize * 2;
        /*
        IO密集型 ：IO密集型任务（大部分时间在等待（网络、磁盘、数据库）；线程在IO等待时不占用CPU，CPU可以执行其他线程。）
        N*2原因：充分利用CPU在IO等待期间的空闲时间通过更多线程重叠IO等待时间，提高CPU利用率（最佳线程数 = CPU核心数 * (1 + 平均等待时间 / 平均计算时间)）
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        int maxPoolSize = corePoolSize * 4;
        补充：CPU密集型
        N+1原因：N个线程充分利用N个CPU核心；+1个线程用于补偿线程阻塞（即使是CPU密集型，也可能有少量阻塞，如内存等待、锁等待）；这个额外线程确保CPU始终忙碌，减少空闲时间
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int maxPoolSize = corePoolSize + 1;
        */

        int keepAliveTime = 60;
        TimeUnit timeUnit = TimeUnit.SECONDS;

        // 任务队列
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(1024);
        // 自定义线程工程
        ThreadFactory customThreadFactory = new CustomThreadFactory(BusinessPoolName);
        RejectedExecutionHandler handler = new CustomRejectionPolicy();
        log.info("创建线程池：{}，核心线程数：{}，最大线程数：{}", BusinessPoolName, corePoolSize, maxPoolSize);
        // 拒绝策略
        return new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, timeUnit, workQueue, customThreadFactory, handler);
    }
}
