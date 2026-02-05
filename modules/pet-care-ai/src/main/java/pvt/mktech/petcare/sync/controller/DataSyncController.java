package pvt.mktech.petcare.sync.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import pvt.mktech.petcare.sync.service.DataMigrationService;

import java.util.Map;

/**
 * {@code @description}: 数据同步接口（Serverless 环境手动触发）
 * {@code @date}: 2026-02-04
 * @author Michael
 */
@Slf4j
@RestController
@RequestMapping("/ai/sync")
@RequiredArgsConstructor
public class DataSyncController {

    private final DataMigrationService dataMigrationService;

    /**
     * 全量同步 Post 数据到 ES
     */
    @PostMapping("/posts/migrate")
    public Map<String, Object> migratePosts() {
        log.info("手动触发 Post 全量同步");
        dataMigrationService.migrateAllPosts();
        return Map.of(
                "code", 200,
                "message", "Post 同步任务已完成"
        );
    }

    /**
     * 全量同步 Activity 数据到 ES
     */
    @PostMapping("/activities/migrate")
    public Map<String, Object> migrateActivities() {
        log.info("手动触发 Activity 全量同步");
        dataMigrationService.migrateAllActivities();
        return Map.of(
                "code", 200,
                "message", "Activity 同步任务已完成"
        );
    }
}
