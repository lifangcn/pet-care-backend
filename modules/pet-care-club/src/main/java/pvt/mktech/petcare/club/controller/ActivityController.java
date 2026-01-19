package pvt.mktech.petcare.club.controller;

import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pvt.mktech.petcare.club.dto.request.ActivityQueryRequest;
import pvt.mktech.petcare.club.entity.Activity;
import pvt.mktech.petcare.club.entity.Post;
import pvt.mktech.petcare.club.service.ActivityService;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.common.usercache.UserContext;

import java.util.List;

/**
 * 活动 控制层。
 */
@Tag(name = "活动管理", description = "活动发布、报名、打卡相关接口")
@RestController
@RequestMapping("/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @PostMapping
    @Operation(summary = "创建活动")
    public Result<Activity> createActivity(@RequestBody Activity activity) {
        activity.setUserId(UserContext.getUserId());
        return Result.success(activityService.createActivity(activity));
    }

    @GetMapping
    @Operation(summary = "活动列表")
    public Result<Page<Activity>> getActivityList(ActivityQueryRequest request) {
        return Result.success(activityService.getActivityList(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "活动详情")
    public Result<Activity> getActivityDetail(@PathVariable Long id) {
        return Result.success(activityService.getActivityDetail(id));
    }

    @PostMapping("/{id}/join")
    @Operation(summary = "报名活动")
    public Result<Boolean> joinActivity(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return Result.success(activityService.joinActivity(userId, id));
    }

    @GetMapping("/{id}/checkins")
    @Operation(summary = "获取打卡动态")
    public Result<List<Post>> getActivityCheckins(@PathVariable Long id) {
        return Result.success(activityService.getActivityCheckins(id));
    }

    @PostMapping("/{id}/checkin")
    @Operation(summary = "活动打卡")
    public Result<Post> checkinActivity(@PathVariable Long id, @RequestBody Post post) {
        Long userId = UserContext.getUserId();
        return Result.success(activityService.checkinActivity(userId, id, post));
    }
}
