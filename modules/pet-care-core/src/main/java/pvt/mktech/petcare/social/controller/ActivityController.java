package pvt.mktech.petcare.social.controller;

import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pvt.mktech.petcare.social.dto.request.ActivityQueryRequest;
import pvt.mktech.petcare.social.dto.request.PostQueryRequest;
import pvt.mktech.petcare.social.entity.Activity;
import pvt.mktech.petcare.social.entity.Post;
import pvt.mktech.petcare.social.service.ActivityService;
import pvt.mktech.petcare.social.service.PostService;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.common.web.UserContext;

import java.util.List;

@Tag(name = "活动管理", description = "活动发布、报名、打卡相关接口")
@RestController
@RequestMapping("/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;
    private final PostService postService;

    @PostMapping
    @Operation(summary = "创建活动")
    public Result<Activity> createActivity(@RequestBody Activity activity) {
        activity.setUserId(UserContext.getUserId());
        return Result.success(activityService.createActivity(activity));
    }

    @PostMapping("/page")
    @Operation(summary = "活动列表")
    public Result<Page<Activity>> pageActivity(@RequestParam(value = "pageNumber", defaultValue = "1") Long pageNumber,
                                               @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize,
                                               @RequestBody ActivityQueryRequest request) {
        return Result.success(activityService.getActivityList(pageNumber, pageSize, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "活动详情")
    public Result<Activity> getActivityDetail(@PathVariable("id") Long id) {
        return Result.success(activityService.getActivityDetail(id));
    }

    @PostMapping("/{id}/checkedIn/page")
    @Operation(summary = "获取打卡动态")
    public Result<Page<Post>> pageActivityCheckedIn(@PathVariable("id") Long id,
                                                    @RequestParam(value = "pageNumber", defaultValue = "1") Long pageNumber,
                                                    @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize) {
        PostQueryRequest postQueryRequest = new PostQueryRequest();
        postQueryRequest.setActivityId(id); // 活动ID
        postQueryRequest.setPostType(5); // 查询打卡信息
        Page<Post> pageByQueryRequest = postService.findPageByQueryRequest(pageNumber, pageSize, postQueryRequest);
        return Result.success(pageByQueryRequest);
    }

    @PostMapping("/{id}/checkIn")
    @Operation(summary = "打卡活动")
    public Result<Post> checkInActivity(@PathVariable("id") Long id, @RequestBody Post post) {
        Long userId = UserContext.getUserId();
        return Result.success(activityService.checkInActivity(userId, id, post));
    }

    @PostMapping("/{id}/join")
    @Operation(summary = "报名活动")
    public Result<Boolean> joinActivity(@PathVariable("id") Long id) {
        Long userId = UserContext.getUserId();
        return Result.success(activityService.joinActivity(userId, id));
    }

    @GetMapping("/{id}/participants")
    @Operation(summary = "获取活动的参与者")
    public Result<List<Post>> listParticipants(@PathVariable("id") Long id) {
        return Result.success(postService.listParticipantsByActivityId(id));
    }
}
