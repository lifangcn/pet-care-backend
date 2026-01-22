package pvt.mktech.petcare.social.controller;

import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pvt.mktech.petcare.social.dto.request.PostQueryRequest;
import pvt.mktech.petcare.social.dto.request.PostSaveRequest;
import pvt.mktech.petcare.social.dto.response.PostDetailResponse;
import pvt.mktech.petcare.social.entity.Post;
import pvt.mktech.petcare.social.service.PostService;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.common.usercache.UserContext;

@Tag(name = "动态管理", description = "动态发布、互动相关接口")
@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    @Operation(summary = "发布动态")
    public Result<Post> savePost(@RequestBody PostSaveRequest request) {
        return Result.success(postService.savePost(request));
    }

    @PostMapping("/page")
    @Operation(summary = "内容广场分页查询动态")
    public Result<Page<Post>> pagePost(@RequestParam(value = "pageNumber", defaultValue = "1") Long pageNumber,
                                       @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize,
                                       @RequestBody PostQueryRequest request) {
        // 查询
        return Result.success(postService.findPageByQueryRequest(pageNumber, pageSize, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "动态详情")
    public Result<PostDetailResponse> getPostDetail(@PathVariable("id") Long id) {
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
        return Result.success(postService.toggleLike(id));
    }

    @PostMapping("/{id}/rate")
    @Operation(summary = "评分")
    public Result<Boolean> rate(@PathVariable("id") Long id, @RequestParam("ratingValue") Integer ratingValue) {
        if (ratingValue < 1 || ratingValue > 5) {
            return Result.error("400", "评分必须在1-5之间");
        }
        return Result.success(postService.rate(id, ratingValue));
    }
}
