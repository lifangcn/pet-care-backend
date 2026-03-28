package pvt.mktech.petcare.sync.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.sync.service.DataMigrationService;
import pvt.mktech.petcare.sync.service.IndexAdminService;

import java.util.Map;

/**
 * 后台数据同步管理控制器
 *
 * @author Michael Li
 * @since 2026-03-27
 */
@Slf4j
@Tag(name = "后台数据同步管理", description = "后台数据同步管理相关接口")
@RestController
@RequestMapping("/admin/ai/sync")
@RequiredArgsConstructor
public class AdminDataSyncController {

    private final DataMigrationService dataMigrationService;
    private final IndexAdminService indexAdminService;

    /**
     * 全量同步 Post 数据到 ES
     *
     * @return 是否成功
     * @author Michael Li
     * @since 2026-03-27
     */
    @PostMapping("/posts/full")
    @Operation(summary = "全量同步 Post 数据")
    public Result<String> fullSyncPosts() {
        log.info("后台触发 Post 全量同步");
        dataMigrationService.migrateAllPosts();
        return Result.success("Post 全量同步任务已触发");
    }

    /**
     * 增量同步 Post 数据
     *
     * @return 是否成功
     * @author Michael Li
     * @since 2026-03-27
     */
    @PostMapping("/posts/incremental")
    @Operation(summary = "增量同步 Post 数据")
    public Result<String> incrementalSyncPosts() {
        log.info("后台触发 Post 增量同步（本期暂未实现具体增量逻辑）");
        return Result.success("增量同步功能待实现");
    }

    /**
     * 查询索引状态
     *
     * @return 索引状态
     * @author Michael Li
     * @since 2026-03-27
     */
    @GetMapping("/index/status")
    @Operation(summary = "查询索引状态")
    public Result<Map<String, Boolean>> getIndexStatus() {
        return Result.success(Map.of(
                "knowledge_document", indexAdminService.indexExists("knowledge_document"),
                "post", indexAdminService.indexExists("post"),
                "activity", indexAdminService.indexExists("activity")
        ));
    }

    /**
     * 重建索引
     *
     * @return 是否成功
     * @author Michael Li
     * @since 2026-03-27
     */
    @PostMapping("/index/rebuild")
    @Operation(summary = "重建索引")
    public Result<String> rebuildIndex() {
        log.info("后台触发索引重建");
        indexAdminService.initAllIndices();
        return Result.success("索引重建任务已触发");
    }
}
