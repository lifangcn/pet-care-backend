package pvt.mktech.petcare.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pvt.mktech.petcare.common.storage.OssTemplate;
import pvt.mktech.petcare.common.usercache.UserContext;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.user.dto.request.UserUpdateRequest;
import pvt.mktech.petcare.shared.dto.CheckInStatsResponse;
import pvt.mktech.petcare.user.dto.response.UserResponse;
import pvt.mktech.petcare.user.service.UserService;

@Tag(name = "用户管理", description = "用户信息管理相关接口")
@RestController
@RequestMapping("/user")
@SecurityRequirement(name = "JWT")
@Slf4j
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final OssTemplate ossTemplate;
    
    @GetMapping("/me")
    @Operation(
        summary = "获取当前用户信息",
        description = "获取当前登录用户的详细信息"
    )
    public ResponseEntity<UserResponse> getCurrentUser() {
        UserResponse user = userService.getUserById(UserContext.getUserId());
        return ResponseEntity.ok(user);
    }

    @PutMapping("/update")
    @Operation(
            summary = "更新当前用户信息",
            description = "更新当前登录用户的基本信息"
    )
    public Result<UserResponse> update(
            @Parameter(description = "用户更新信息", required = true)
            @Valid @RequestBody UserUpdateRequest request) {
        UserResponse updatedUser = userService.updateUser(UserContext.getUserId(), request);
        return Result.success(updatedUser);
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

    @GetMapping("/check-phone")
    @Operation(summary = "检查手机号是否存在")
    public ResponseEntity<Boolean> checkPhoneExists(
            @Parameter(description = "手机号", required = true, example = "13800138000")
            @RequestParam String phone) {
        boolean exists = userService.checkPhoneExists(phone);
        return ResponseEntity.ok(exists);
    }

    @PostMapping(value = "/avatar", consumes = "multipart/form-data")
    @Operation(summary = "上传用户头像")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = UserContext.getUserId();
        // 上传头像到MinIO
        String avatarUrl = ossTemplate.uploadAvatar(file, userId);
        return Result.success(avatarUrl);
    }

    @PostMapping("/checkIn")
    @Operation(summary = "用户签到")
    public Result<Boolean> checkIn() {
        Long userId = UserContext.getUserId();
        return Result.success(userService.checkIn(userId));
    }

    @GetMapping("/checkIn/stats")
    @Operation(summary = "查询用户签到记录")
    public Result<CheckInStatsResponse> checkInStats(@RequestParam("year") Long year,
                                                     @RequestParam("month") Long month) {
        Long userId = UserContext.getUserId();
        return Result.success(userService.getCheckInStats(userId, year, month));
    }
}