package pvt.mktech.petcare.sync.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.CountResponse;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.sync.constants.EsIndexConstants;
import pvt.mktech.petcare.sync.entity.core.ActivityEntity;
import pvt.mktech.petcare.sync.entity.core.PostEntity;
import pvt.mktech.petcare.sync.mapper.core.ActivityMapper;
import pvt.mktech.petcare.sync.mapper.core.PostMapper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * {@code @description}: 数据一致性校验服务
 * <p>对比 MySQL 和 ES 的数据一致性，发现不一致时记录日志并修复</p>
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sync.consistency.enabled", havingValue = "true", matchIfMissing = false)
public class DataConsistencyService {

    private final PostMapper postMapper;
    private final ActivityMapper activityMapper;
    private final ElasticsearchClient elasticsearchClient;
    private final DataMigrationService dataMigrationService;

    /**
     * 实时校验：每小时对比 MySQL 和 ES 的条数
     */
    @Scheduled(cron = "${sync.consistency.realtime.cron:0 0 * * * ?}")
    public void realtimeConsistencyCheck() {
        log.info("开始实时数据一致性校验");

        try {
            // 1. 校验 Post
            checkPostCount();

            // 2. 校验 Activity
            checkActivityCount();

            log.info("实时数据一致性校验完成");

        } catch (Exception e) {
            log.error("实时数据一致性校验失败", e);
        }
    }

    /**
     * 全量校验：每日凌晨执行全量比对
     */
    @Scheduled(cron = "${sync.consistency.full.cron:0 0 1 * * ?}")
    public void fullConsistencyCheck() {
        log.info("开始全量数据一致性校验");

        try {
            // 1. 全量校验 Post
            fullCheckAndRepairPosts();

            // 2. 全量校验 Activity
            fullCheckAndRepairActivities();

            log.info("全量数据一致性校验完成");

        } catch (Exception e) {
            log.error("全量数据一致性校验失败", e);
        }
    }

    /**
     * 校验 Post 数量
     */
    private void checkPostCount() {
        try {
            // MySQL 数量（正常状态的 Post）
            QueryWrapper mysqlQuery = QueryWrapper.create()
                    .where("status = ?", 1);
            long mysqlCount = postMapper.selectCountByQuery(mysqlQuery);

            // ES 数量
            CountRequest esRequest = CountRequest.of(c -> c
                    .index(EsIndexConstants.POST_INDEX)
                    .query(q -> q.term(t -> t.field("status").value(1)))
            );
            CountResponse esResponse = elasticsearchClient.count(esRequest);
            long esCount = esResponse.count();

            if (mysqlCount != esCount) {
                log.warn("Post 数据不一致: MySQL={}, ES={}, 差异={}", mysqlCount, esCount, mysqlCount - esCount);
            } else {
                log.debug("Post 数据一致: MySQL={}, ES={}", mysqlCount, esCount);
            }

        } catch (Exception e) {
            log.error("校验 Post 数量失败", e);
        }
    }

    /**
     * 校验 Activity 数量
     */
    private void checkActivityCount() {
        try {
            // MySQL 数量（招募中的 Activity）
            QueryWrapper mysqlQuery = QueryWrapper.create()
                    .where("status = ?", 1);
            long mysqlCount = activityMapper.selectCountByQuery(mysqlQuery);

            // ES 数量
            CountRequest esRequest = CountRequest.of(c -> c
                    .index(EsIndexConstants.ACTIVITY_INDEX)
                    .query(q -> q.term(t -> t.field("status").value(1)))
            );
            CountResponse esResponse = elasticsearchClient.count(esRequest);
            long esCount = esResponse.count();

            if (mysqlCount != esCount) {
                log.warn("Activity 数据不一致: MySQL={}, ES={}, 差异={}", mysqlCount, esCount, mysqlCount - esCount);
            } else {
                log.debug("Activity 数据一致: MySQL={}, ES={}", mysqlCount, esCount);
            }

        } catch (Exception e) {
            log.error("校验 Activity 数量失败", e);
        }
    }

    /**
     * 全量校验并修复 Post
     */
    private void fullCheckAndRepairPosts() {
        try {
            // 1. 获取 MySQL 所有 Post ID
            List<PostEntity> mysqlPosts = postMapper.selectListByQuery(
                    QueryWrapper.create().where("status = ?", 1)
            );
            Set<Long> mysqlIds = mysqlPosts.stream()
                    .map(PostEntity::getId)
                    .collect(Collectors.toSet());

            // 2. 获取 ES 所有 Post ID
            Set<Long> esIds = fetchEsPostIds();

            // 3. 找出缺失的数据
            Set<Long> missingIds = mysqlIds.stream()
                    .filter(id -> !esIds.contains(id))
                    .collect(Collectors.toSet());

            if (!missingIds.isEmpty()) {
                log.warn("发现 {} 条 Post 数据缺失，开始修复", missingIds.size());
                // 修复缺失数据（重新同步）
                repairMissingPosts(missingIds);
            } else {
                log.info("Post 数据完整，无需修复");
            }

        } catch (Exception e) {
            log.error("全量校验 Post 失败", e);
        }
    }

    /**
     * 全量校验并修复 Activity
     */
    private void fullCheckAndRepairActivities() {
        try {
            // 1. 获取 MySQL 所有 Activity ID
            List<ActivityEntity> mysqlActivities = activityMapper.selectListByQuery(
                    QueryWrapper.create().where("status = ?", 1)
            );
            Set<Long> mysqlIds = mysqlActivities.stream()
                    .map(ActivityEntity::getId)
                    .collect(Collectors.toSet());

            // 2. 获取 ES 所有 Activity ID
            Set<Long> esIds = fetchEsActivityIds();

            // 3. 找出缺失的数据
            Set<Long> missingIds = mysqlIds.stream()
                    .filter(id -> !esIds.contains(id))
                    .collect(Collectors.toSet());

            if (!missingIds.isEmpty()) {
                log.warn("发现 {} 条 Activity 数据缺失，开始修复", missingIds.size());
                // 修复缺失数据（重新同步）
                repairMissingActivities(missingIds);
            } else {
                log.info("Activity 数据完整，无需修复");
            }

        } catch (Exception e) {
            log.error("全量校验 Activity 失败", e);
        }
    }

    /**
     * 获取 ES 中所有 Post ID
     */
    private Set<Long> fetchEsPostIds() {
        // TODO: 实现获取 ES 中所有 Post ID 的逻辑
        // 可以使用 scroll API 或 search_after 分页获取
        return Set.of();
    }

    /**
     * 获取 ES 中所有 Activity ID
     */
    private Set<Long> fetchEsActivityIds() {
        // TODO: 实现获取 ES 中所有 Activity ID 的逻辑
        return Set.of();
    }

    /**
     * 修复缺失的 Post 数据
     */
    private void repairMissingPosts(Set<Long> missingIds) {
        // 调用数据迁移服务重新同步缺失的数据
        // 这里可以优化为批量查询并同步
        log.info("修复缺失的 Post 数据: {}", missingIds);
        // dataMigrationService.migratePostsByIds(missingIds);  // 需要实现此方法
    }

    /**
     * 修复缺失的 Activity 数据
     */
    private void repairMissingActivities(Set<Long> missingIds) {
        log.info("修复缺失的 Activity 数据: {}", missingIds);
        // dataMigrationService.migrateActivitiesByIds(missingIds);  // 需要实现此方法
    }
}
