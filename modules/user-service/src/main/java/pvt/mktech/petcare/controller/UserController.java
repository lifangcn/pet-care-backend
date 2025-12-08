package pvt.mktech.petcare.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.context.UserHolder;
import pvt.mktech.petcare.dto.request.UserUpdateRequest;
import pvt.mktech.petcare.dto.response.UserResponse;
import pvt.mktech.petcare.mapper.UserMapper;
import pvt.mktech.petcare.service.UserService;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "用户管理", description = "用户信息管理相关接口")
//@SecurityRequirement(name = "JWT")
public class UserController {
    
    private final UserService userService;

    private final StringRedisTemplate redisTemplate;

    private final UserMapper userMapper;
    
    @GetMapping("/me")
    @Operation(
        summary = "获取当前用户信息",
        description = "获取当前登录用户的详细信息"
    )
    public ResponseEntity<UserResponse> getCurrentUser(
            @Parameter(hidden = true) // 隐藏这个参数，因为它从token中获取
            @RequestAttribute Long userId) {
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }
    
    @GetMapping("/{userId}")
    @Operation(
        summary = "根据ID获取用户信息",
        description = "根据用户ID获取用户公开信息"
    )
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "用户ID", required = true, example = "1")
            @PathVariable Long userId) {
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }
    
    @PutMapping("/profile")
    @Operation(
        summary = "更新当前用户信息",
        description = "更新当前登录用户的基本信息"
    )
    public Result<UserResponse> updateCurrentUser(
            @Parameter(description = "用户更新信息", required = true)
            @Valid @RequestBody UserUpdateRequest request) {
        UserResponse updatedUser = userService.updateUser(UserHolder.getUser().getId(), request);
        return Result.ok(updatedUser);
    }
    
    @PostMapping("/change-password")
    @Operation(
        summary = "修改密码",
        description = "修改当前登录用户的密码"
    )
    public ResponseEntity<Void> changePassword(
            @Parameter(hidden = true)
            @RequestAttribute Long userId,
            @Parameter(description = "旧密码", required = true, example = "oldPassword123")
            @RequestParam String oldPassword,
            @Parameter(description = "新密码", required = true, example = "newPassword456")
            @RequestParam String newPassword) {
        userService.changePassword(userId, oldPassword, newPassword);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check-username")
    @Operation(
        summary = "检查用户名是否存在",
        description = "检查用户名是否已被注册"
    )
    public ResponseEntity<Boolean> checkUsernameExists(
            @Parameter(description = "用户名", required = true, example = "john_doe")
            @RequestParam String username) {
        boolean exists = userService.checkUsernameExists(username);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/check-email")
    @Operation(
        summary = "检查邮箱是否存在",
        description = "检查邮箱是否已被注册"
    )
    public ResponseEntity<Boolean> checkEmailExists(
            @Parameter(description = "邮箱地址", required = true, example = "user@example.com")
            @RequestParam String email) {
        boolean exists = userService.checkEmailExists(email);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/check-phone")
    @Operation(
        summary = "检查手机号是否存在",
        description = "检查手机号是否已被注册"
    )
    public ResponseEntity<Boolean> checkPhoneExists(
            @Parameter(description = "手机号", required = true, example = "13800138000")
            @RequestParam String phone) {
        boolean exists = userService.checkPhoneExists(phone);
        return ResponseEntity.ok(exists);
    }
}