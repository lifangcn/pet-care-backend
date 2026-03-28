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
import pvt.mktech.petcare.social.entity.Post;
import pvt.mktech.petcare.social.service.PostService;

/**
 * 后台动态审核控制器
 *
 * @author Michael Li
 * @since 2026-03-27
 */
@Tag(name = "后台动态审核", description = "后台动态审核相关接口")
@RestController
@RequireAdmin
@RequestMapping("/admin/post")
@RequiredArgsConstructor
public class AdminPostController {

    private final PostService postService;

    /**
     * 查询待审核动态列表
     *
     * @param pageNumber 页码
     * @param pageSize 页大小
     * @return 待审核动态分页结果
     * @author Michael Li
     * @since 2026-03-27
     */
    @GetMapping("/pending")
    @Operation(summary = "查询待审核动态列表")
    public Result<Page<Post>> pagePendingPosts(@RequestParam(value = "pageNumber", defaultValue = "1") Long pageNumber,
                                               @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize) {
        return Result.success(postService.pagePendingPosts(pageNumber, pageSize));
    }

    /**
     * 审核通过动态
     *
     * @param id 动态ID
     * @return 是否成功
     * @author Michael Li
     * @since 2026-03-27
     */
    @PutMapping("/{id}/approve")
    @Operation(summary = "审核通过动态")
    public Result<Boolean> approvePost(@PathVariable("id") Long id) {
        return Result.success(postService.approvePost(id));
    }

    /**
     * 审核拒绝动态
     *
     * @param id 动态ID
     * @return 是否成功
     * @author Michael Li
     * @since 2026-03-27
     */
    @PutMapping("/{id}/reject")
    @Operation(summary = "审核拒绝动态")
    public Result<Boolean> rejectPost(@PathVariable("id") Long id) {
        return Result.success(postService.rejectPost(id));
    }
}
