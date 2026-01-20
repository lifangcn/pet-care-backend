package pvt.mktech.petcare.core;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.springframework.boot.test.context.SpringBootTest;
import pvt.mktech.petcare.common.redis.DistributedIdGenerator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;

@SpringBootTest(classes = CoreServiceApplication.class)
public class UserServiceApplicationTest {

    @Resource
    private DistributedIdGenerator distributedIdGenerator;
    @Resource
    private ThreadPoolExecutor coreThreadPoolExecutor;

    //    @Test
    void testGenerateIdConcurrency() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(100);
        ConcurrentHashMap<Long, Boolean> ids = new ConcurrentHashMap<>();
        long start = System.currentTimeMillis();

        for (int i = 0; i < 100; i++) {
            coreThreadPool.submit(() -> {
                for (int j = 0; j < 100; j++) {
                    Long id = distributedIdGenerator.generateId("test");
                    ids.put(id, true);
                    System.out.println(Thread.currentThread().getName() + "生成ID：" + id);
                }
                countDownLatch.countDown();
            });
        }

        countDownLatch.await();
        long end = System.currentTimeMillis();

        System.out.println("并发测试：生成10000个ID，耗时：" + (end - start) + "ms");
        Assertions.assertEquals(10000, ids.size());
    }
}

