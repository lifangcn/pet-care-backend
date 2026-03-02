package pvt.mktech.petcare.sync.schedule;

import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.sync.entity.core.ActivityEntity;
import pvt.mktech.petcare.sync.entity.core.PostEntity;
import pvt.mktech.petcare.sync.mapper.core.ActivityMapper;
import pvt.mktech.petcare.sync.mapper.core.PostMapper;
import pvt.mktech.petcare.sync.service.SyncService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code @description}: 高频字段定时同步任务
 * <p>定时从 MySQL 查询高频字段数据并批量更新到 ES</p>
 * {@code @date}: 2026-03-01
 * @author Michael
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HighFreqFieldSyncScheduler {

    private final PostMapper postMapper;
    private final ActivityMapper activityMapper;
    private final SyncService syncService;

    /**
     * 每5分钟同步一次 Post 高频字段
     */
    @Scheduled(fixedRate = 300_000)
    public void syncPostHighFreqFields() {
        try {
            log.info("开始同步 Post 高频字段到 ES");

            // 查询所有 enabled=1 的 Post（只查询高频字段）
            List<PostEntity> posts = postMapper.selectListByQuery(
                QueryWrapper.create()
                    .select("id", "view_count", "like_count", "rating_avg", "rating_count", "rating_total")
                    .where("enabled = 1")
                    .limit(1000)
            );

            int successCount = 0;
            for (PostEntity post : posts) {
                try {
                    // 构建部分更新文档
                    Map<String, Object> partialDoc = new HashMap<>();
                    partialDoc.put("view_count", post.getViewCount());
                    partialDoc.put("like_count", post.getLikeCount());
                    partialDoc.put("rating_avg", post.getRatingAvg());
                    partialDoc.put("rating_count", post.getRatingCount());
                    partialDoc.put("rating_total", post.getRatingTotal());

                    syncService.partialUpdate("posts", String.valueOf(post.getId()), partialDoc);
                    successCount++;
                } catch (Exception e) {
                    log.error("Post 高频字段同步失败: id={}", post.getId(), e);
                }
            }

            log.info("Post 高频字段同步完成: 成功={}/{}", successCount, posts.size());
        } catch (Exception e) {
            log.error("Post 高频字段同步任务执行失败", e);
        }
    }

    /**
     * 每5分钟同步一次 Activity 高频字段
     */
    @Scheduled(fixedRate = 300_000)
    public void syncActivityHighFreqFields() {
        try {
            log.info("开始同步 Activity 高频字段到 ES");

            // 查询所有 enabled=1 的 Activity（只查询高频字段）
            List<ActivityEntity> activities = activityMapper.selectListByQuery(
                QueryWrapper.create()
                    .select("id", "current_participants", "check_in_count")
                    .where("status != 'CANCELLED'")
                    .limit(1000)
            );

            int successCount = 0;
            for (ActivityEntity activity : activities) {
                try {
                    // 构建部分更新文档
                    Map<String, Object> partialDoc = new HashMap<>();
                    partialDoc.put("current_participants", activity.getCurrentParticipants());
                    partialDoc.put("check_in_count", activity.getCheckInCount());

                    syncService.partialUpdate("activities", String.valueOf(activity.getId()), partialDoc);
                    successCount++;
                } catch (Exception e) {
                    log.error("Activity 高频字段同步失败: id={}", activity.getId(), e);
                }
            }

            log.info("Activity 高频字段同步完成: 成功={}/{}", successCount, activities.size());
        } catch (Exception e) {
            log.error("Activity 高频字段同步任务执行失败", e);
        }
    }
}
