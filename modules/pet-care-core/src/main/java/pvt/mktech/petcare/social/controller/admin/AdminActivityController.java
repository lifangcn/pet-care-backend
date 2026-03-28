package pvt.mktech.petcare.social.controller.admin;

import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pvt.mktech.petcare.admin.security.RequireAdmin;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.social.entity.Activity;
import pvt.mktech.petcare.social.service.ActivityService;

/**
 * 后台活动审核控制器
 *
 * @author Michael Li
 * @since 2026-03-27
 */
@Tag(name = "后台活动审核", description = "后台活动审核相关接口")
@RestController
@RequireAdmin
@RequestMapping("/admin/activity")
@RequiredArgsConstructor
public class AdminActivityController {

    private final ActivityService activityService;

    /**
     * 查询待审核活动列表
     *
     * @param pageNumber 页码
     * @param pageSize 页大小
     * @return 待审核活动分页结果
     * @author Michael Li
     * @since 2026-03-27
     */
    @GetMapping("/pending")
    @Operation(summary = "查询待审核活动列表")
    public Result<Page<Activity>> pagePendingActivities(@RequestParam(value = "pageNumber", defaultValue = "1") Long pageNumber,
                                                        @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize) {
        return Result.success(activityService.pagePendingActivities(pageNumber, pageSize));
    }

    /**
     * 审核通过活动
     *
     * @param id 活动ID
     * @return 是否成功
     * @author Michael Li
     * @since 2026-03-27
     */
    @PutMapping("/{id}/approve")
    @Operation(summary = "审核通过活动")
    public Result<Boolean> approveActivity(@PathVariable("id") Long id) {
        return Result.success(activityService.approveActivity(id));
    }

    /**
     * 审核拒绝活动
     *
     * @param id 活动ID
     * @return 是否成功
     * @author Michael Li
     * @since 2026-03-27
     */
    @PutMapping("/{id}/reject")
    @Operation(summary = "审核拒绝活动")
    public Result<Boolean> rejectActivity(@PathVariable("id") Long id) {
        return Result.success(activityService.rejectActivity(id));
    }
}
