package pvt.mktech.petcare.sync.service;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.datasource.DataSourceKey;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.sync.constants.EsIndexConstants;
import pvt.mktech.petcare.sync.converter.ActivityEntityConverter;
import pvt.mktech.petcare.sync.converter.PostEntityConverter;
import pvt.mktech.petcare.sync.entity.core.ActivityEntity;
import pvt.mktech.petcare.sync.entity.core.PostEntity;
import pvt.mktech.petcare.sync.mapper.core.ActivityMapper;
import pvt.mktech.petcare.sync.mapper.core.PostMapper;

import java.util.List;

/**
 * {@code @description}: 数据迁移服务
 * <p>全量迁移 Post 和 Activity 数据到 ES</p>
 * {@code @date}: 2026-01-30
 *
 * @author Michael
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataMigrationService {

    private final PostMapper postMapper;
    private final ActivityMapper activityMapper;
    private final SyncService syncService;
    private final PostEntityConverter postEntityConverter;
    private final ActivityEntityConverter activityEntityConverter;

    private static final int BATCH_SIZE = 100;

    /**
     * 全量迁移 Post 数据
     */
    public void migrateAllPosts() {
        log.info("开始全量迁移 Post 数据到 ES");
        int pageNumber = 1;
        int totalMigrated = 0;

        while (true) {
            QueryWrapper queryWrapper = QueryWrapper.create();
            Page<PostEntity> page = postMapper.paginate(Page.of((long) pageNumber, (long) BATCH_SIZE), queryWrapper);
            List<PostEntity> posts = page.getRecords();
            if (posts.isEmpty()) {
                break;
            }

            for (PostEntity post : posts) {
                try {
                    var document = postEntityConverter.convert(post);
                    syncService.upsert(EsIndexConstants.POST_INDEX, String.valueOf(post.getId()), document);
                    totalMigrated++;
                } catch (Exception e) {
                    log.error("迁移 Post 失败: id={}", post.getId(), e);
                }
            }

            log.info("已迁移 Post: {}/{}", totalMigrated, page.getTotalRow());

            // 判断是否还有下一页
            if (pageNumber >= page.getTotalPage()) {
                break;
            }
            pageNumber++;
        }

        log.info("Post 全量迁移完成，共迁移 {} 条", totalMigrated);
    }

    /**
     * 全量迁移 Activity 数据
     */
    public void migrateAllActivities() {
        log.info("开始全量迁移 Activity 数据到 ES");
        int pageNumber = 1;
        int totalMigrated = 0;

        while (true) {
            QueryWrapper queryWrapper = QueryWrapper.create();
            Page<ActivityEntity> page = activityMapper.paginate(Page.of((long) pageNumber, (long) BATCH_SIZE), queryWrapper);
            List<ActivityEntity> activities = page.getRecords();

            if (activities.isEmpty()) {
                break;
            }

            for (ActivityEntity activity : activities) {
                try {
                    var document = activityEntityConverter.convert(activity);
                    syncService.upsert(EsIndexConstants.ACTIVITY_INDEX, String.valueOf(activity.getId()), document);
                    totalMigrated++;
                } catch (Exception e) {
                    log.error("迁移 Activity 失败: id={}", activity.getId(), e);
                }
            }

            log.info("已迁移 Activity: {}/{}", totalMigrated, page.getTotalRow());

            // 判断是否还有下一页
            if (pageNumber >= page.getTotalPage()) {
                break;
            }
            pageNumber++;
        }

        log.info("Activity 全量迁移完成，共迁移 {} 条", totalMigrated);
    }

}
