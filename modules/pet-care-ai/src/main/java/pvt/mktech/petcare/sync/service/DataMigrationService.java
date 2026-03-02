package pvt.mktech.petcare.sync.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.sync.dto.EsActivityDocument;
import pvt.mktech.petcare.sync.dto.EsPostDocument;
import pvt.mktech.petcare.sync.entity.core.ActivityEntity;
import pvt.mktech.petcare.sync.entity.core.PostEntity;
import pvt.mktech.petcare.sync.mapper.core.ActivityMapper;
import pvt.mktech.petcare.sync.mapper.core.PostMapper;
import pvt.mktech.petcare.sync.util.DateTimeConverter;

import java.util.List;

import static pvt.mktech.petcare.sync.constants.SyncConstants.ACTIVITY_INDEX;
import static pvt.mktech.petcare.sync.constants.SyncConstants.POST_INDEX;

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
    private final ObjectMapper objectMapper;

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
            Page<PostEntity> page = postMapper.paginate(Page.of(pageNumber, BATCH_SIZE), queryWrapper);
            List<PostEntity> posts = page.getRecords();
            if (posts.isEmpty()) {
                break;
            }

            for (PostEntity post : posts) {
                try {
                    EsPostDocument doc = convertToPostDocument(post);
                    syncService.upsert(POST_INDEX, String.valueOf(post.getId()), doc);
                    totalMigrated++;
                } catch (Exception e) {
                    log.error("迁移 Post 失败: id={}", post.getId(), e);
                }
            }

            log.info("已迁移 Post: {}/{}", totalMigrated, page.getTotalRow());

            if (pageNumber >= page.getTotalPage()) {
                break;
            }
            pageNumber++;
        }

        log.info("Post 全量迁移完成，共迁移 {} 条", totalMigrated);
    }

    /**
     * Entity → Document 转换（Post）
     */
    private EsPostDocument convertToPostDocument(PostEntity entity) {
        EsPostDocument doc = BeanUtil.copyProperties(entity, EsPostDocument.class);
        // 日期转换
        doc.setCreatedAt(DateTimeConverter.toInstant(entity.getCreatedAt()));
        // JSON 字符串解析
        if (entity.getMediaUrls() != null && !entity.getMediaUrls().isEmpty()) {
            try {
                doc.setMediaUrls(objectMapper.readValue(entity.getMediaUrls(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
            } catch (Exception e) {
                log.warn("解析 media_urls 失败: {}", entity.getMediaUrls(), e);
            }
        }
        return doc;
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
            Page<ActivityEntity> page = activityMapper.paginate(Page.of(pageNumber, BATCH_SIZE), queryWrapper);
            List<ActivityEntity> activities = page.getRecords();

            if (activities.isEmpty()) {
                break;
            }

            for (ActivityEntity activity : activities) {
                try {
                    EsActivityDocument doc = convertToActivityDocument(activity);
                    syncService.upsert(ACTIVITY_INDEX, String.valueOf(activity.getId()), doc);
                    totalMigrated++;
                } catch (Exception e) {
                    log.error("迁移 Activity 失败: id={}", activity.getId(), e);
                }
            }

            log.info("已迁移 Activity: {}/{}", totalMigrated, page.getTotalRow());

            if (pageNumber >= page.getTotalPage()) {
                break;
            }
            pageNumber++;
        }

        log.info("Activity 全量迁移完成，共迁移 {} 条", totalMigrated);
    }

    /**
     * Entity → Document 转换（Activity）
     */
    private EsActivityDocument convertToActivityDocument(ActivityEntity entity) {
        EsActivityDocument doc = BeanUtil.copyProperties(entity, EsActivityDocument.class);
        // 日期转换
        doc.setActivityTime(DateTimeConverter.toInstant(entity.getActivityTime()));
        doc.setEndTime(DateTimeConverter.toInstant(entity.getEndTime()));
        doc.setCreatedAt(DateTimeConverter.toInstant(entity.getCreatedAt()));
        return doc;
    }
}
