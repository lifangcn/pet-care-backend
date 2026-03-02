package pvt.mktech.petcare.social.schedule;

import com.mybatisflex.core.update.UpdateChain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.common.redis.DistributedLock;
import pvt.mktech.petcare.common.redis.RedisUtil;
import pvt.mktech.petcare.infrastructure.constant.CoreConstant;
import pvt.mktech.petcare.social.entity.Post;
import pvt.mktech.petcare.social.mapper.PostMapper;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * {@code @description}: 浏览量定时同步任务
 * <p>定时将 Redis 中的浏览量计数批量同步到 MySQL</p>
 * {@code @date}: 2026-03-02
 * @author Michael Li
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountSyncScheduler {

    private final PostMapper postMapper;
    private final RedisUtil redisUtil;

    /**
     * 每30分钟同步一次浏览量到数据库
     */
    @Scheduled(fixedRate = 1_800_000)
    @DistributedLock(
        lockKey = CoreConstant.CORE_POST_VIEW_COUNT_SYNC_LOCK_KEY,
        leaseTime = 60000,
        waitTime = 5000,
        timeUnit = TimeUnit.MILLISECONDS
    )
    public void syncViewCounts() {
        try {
            log.info("开始同步 Post 浏览量到数据库");

            // 1. 扫描所有浏览量计数器 key（使用 scan 避免 Redis 阻塞）
            Collection<String> countKeys = redisUtil.scan(CoreConstant.CORE_POST_VIEW_COUNT_KEY_PREFIX + "*", 100);
            if (countKeys.isEmpty()) {
                log.debug("没有待同步的浏览量数据");
                return;
            }

            int successCount = 0;
            int skipCount = 0;

            for (String countKey : countKeys) {
                try {
                    // 从 key 中提取 postId（格式：core:post:view_count:{postId}）
                    String postIdStr = countKey.substring(countKey.lastIndexOf(':') + 1);
                    Long postId = Long.valueOf(postIdStr);

                    // 获取 Redis 中的增量
                    long delta = redisUtil.getAtomicLong(countKey);
                    if (delta <= 0) {
                        continue;
                    }

                    // 查询数据库当前值
                    Post post = postMapper.selectOneById(postId);
                    if (post == null) {
                        log.warn("Post 不存在，跳过同步: postId={}", postId);
                        redisUtil.delete(countKey);
                        continue;
                    }

                    // 批量更新：当前值 + Redis 增量
                    long newViewCount = (post.getViewCount() == null ? 0 : post.getViewCount()) + delta;
                    UpdateChain.of(Post.class)
                        .set(Post::getViewCount, newViewCount)
                        .where(Post::getId).eq(postId)
                        .update();

                    // 清理已同步的计数器
                    redisUtil.delete(countKey);
                    successCount++;
                } catch (NumberFormatException e) {
                    log.error("解析 postId 失败: key={}", countKey, e);
                } catch (Exception e) {
                    log.error("同步浏览量失败: key={}", countKey, e);
                }
            }

            log.info("Post 浏览量同步完成: 成功={}, 跳过={}", successCount, skipCount);
        } catch (Exception e) {
            log.error("Post 浏览量同步任务执行失败", e);
        }
    }
}
