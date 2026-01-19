package pvt.mktech.petcare.club.controller;

import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pvt.mktech.petcare.club.dto.request.PostQueryRequest;
import pvt.mktech.petcare.club.entity.Post;
import pvt.mktech.petcare.club.service.InteractionService;
import pvt.mktech.petcare.club.service.PostService;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.common.usercache.UserContext;

import java.math.BigDecimal;

/**
 * 动态 控制层。
 */
@Tag(name = "动态管理", description = "动态发布、互动相关接口")
@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final InteractionService interactionService;

    @PostMapping
    @Operation(summary = "发布动态")
    public Result<Post> savePost(@RequestBody Post post) {
        post.setUserId(UserContext.getUserId());
        return Result.success(postService.savePost(post));
    }

    @GetMapping
    @Operation(summary = "动态列表")
    public Result<Page<Post>> getPostList(PostQueryRequest request) {
        return Result.success(postService.getPostList(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "动态详情")
    public Result<Post> getPostDetail(@PathVariable("id") Long id) {
        return Result.success(postService.getPostDetail(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑动态")
    public Result<Boolean> updatePost(@PathVariable("id") Long id, @RequestBody Post post) {
        return Result.success(postService.updatePost(id, post));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除动态")
    public Result<Boolean> deletePost(@PathVariable("id") Long id) {
        return Result.success(postService.deletePost(id));
    }

    @PostMapping("/{id}/like")
    @Operation(summary = "点赞/取消点赞")
    public Result<Boolean> toggleLike(@PathVariable("id") Long id) {
        Long userId = UserContext.getUserId();
        return Result.success(interactionService.toggleLike(userId, id));
    }

    @PostMapping("/{id}/rate")
    @Operation(summary = "评分")
    public Result<Boolean> rate(@PathVariable("id") Long id, @RequestParam Integer rating) {
        Long userId = UserContext.getUserId();
        if (rating < 1 || rating > 5) {
            return Result.error("400", "评分必须在1-5之间");
        }
        return Result.success(interactionService.rate(userId, id, rating));
    }

    @GetMapping("/{id}/ratings")
    @Operation(summary = "评分详情")
    public Result<BigDecimal> getRatingDetail(@PathVariable("id") Long id) {
        return Result.success(interactionService.getRatingAvg(id));
    }
}
