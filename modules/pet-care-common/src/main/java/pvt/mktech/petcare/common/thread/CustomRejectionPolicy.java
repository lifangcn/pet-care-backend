package pvt.mktech.petcare.common.thread;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * {@code @description}: 阿里巴巴规范要求：线程池必须自定义拒绝策略
 * {@code @date}: 2025/12/1 13:41
 *
 * @author Michael
 */
@Slf4j
public class CustomRejectionPolicy implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        // 线程池关闭时的拒绝是正常行为，不记录 ERROR
        if (executor.isShutdown()) {
            log.debug("线程池正在关闭，任务被忽略，线程池信息：{}", executor.toString());
            return;
        }
        log.error("任务被拒绝， 线程池信息：{}", executor.toString());
        try {
            boolean offered = executor.getQueue().offer(r, 60, TimeUnit.SECONDS);
            if (!offered) {
                log.error("等待60秒仍然无法执行任务，任务将被丢弃");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("重新提交任务时被中断", e);
        }
    }
}
