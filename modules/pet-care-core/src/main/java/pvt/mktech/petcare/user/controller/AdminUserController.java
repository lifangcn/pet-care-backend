package pvt.mktech.petcare.user.controller;

import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pvt.mktech.petcare.admin.security.RequireAdmin;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.common.web.UserContext;
import pvt.mktech.petcare.user.dto.request.AdminUserEnabledUpdateRequest;
import pvt.mktech.petcare.user.dto.request.AdminUserRoleUpdateRequest;
import pvt.mktech.petcare.user.dto.response.AdminUserResponse;
import pvt.mktech.petcare.user.service.UserService;

/**
 * 后台用户管理控制器
 *
 * @author Michael Li
 * @since 2026-03-27
 */
@Tag(name = "后台用户管理", description = "后台用户管理相关接口")
@RestController
@RequireAdmin
@RequestMapping("/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    /**
     * 获取当前管理员用户信息
     *
     * @return 当前管理员用户信息
     * @author Michael Li
     * @since 2026-03-28
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前管理员用户信息")
    public Result<AdminUserResponse> getCurrentAdminUser() {
        return Result.success(userService.getAdminUserById(UserContext.getUserId()));
    }

    /**
     * 分页查询用户列表
     *
     * @param pageNumber 页码
     * @param pageSize 页大小
     * @return 用户分页结果
     * @author Michael Li
     * @since 2026-03-27
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询用户列表")
    public Result<Page<AdminUserResponse>> pageAdminUsers(@RequestParam(value = "pageNumber", defaultValue = "1") Long pageNumber,
                                                          @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize) {
        return Result.success(userService.pageAdminUsers(pageNumber, pageSize));
    }

    /**
     * 更新用户管理员角色
     *
     * @param id 用户ID
     * @param request 更新管理员角色请求
     * @return 是否成功
     * @author Michael Li
     * @since 2026-03-27
     */
    @PutMapping("/{id}/role")
    @Operation(summary = "更新用户管理员角色")
    public Result<Boolean> updateAdminRole(@PathVariable("id") Long id,
                                          @RequestBody AdminUserRoleUpdateRequest request) {
        return Result.success(userService.updateAdminRole(id, request.getIsAdmin()));
    }

    /**
     * 更新用户启用状态
     *
     * @param id 用户ID
     * @param request 更新启用状态请求
     * @return 是否成功
     * @author Michael Li
     * @since 2026-03-27
     */
    @PutMapping("/{id}/enabled")
    @Operation(summary = "更新用户启用状态")
    public Result<Boolean> updateEnabledStatus(@PathVariable("id") Long id,
                                               @RequestBody AdminUserEnabledUpdateRequest request) {
        return Result.success(userService.updateEnabledStatus(id, request.getEnabled()));
    }
}