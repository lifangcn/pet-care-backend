package pvt.mktech.petcare.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.context.UserHolder;
import pvt.mktech.petcare.dto.LoginInfoDto;
import pvt.mktech.petcare.dto.request.LoginRequest;
import pvt.mktech.petcare.service.AuthService;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户登录、注册、登出等相关接口")
public class AuthController {
    
    private final AuthService authService;

    @PostMapping("/code")
    @Operation(
            summary = "请求手机验证码"
    )
    public Result<String> code(
            @Parameter(description = "手机号", required = true)
            @Valid @RequestParam("phone") String phone, HttpSession session) {
        return authService.sendCode(phone, session);
    }
    
    @PostMapping("/login")
    @Operation(
        summary = "用户登录，确认后发送验证码，填写所有信息完毕后，如果未注册自动注册",
        description = "支持用户名、邮箱、手机号登录"
    )
    public Result<LoginInfoDto> login(
            @Parameter(description = "登录请求参数", required = true)
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        return authService.login(request, clientIp);
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "获取当前用户信息")
    public Result<LoginInfoDto> me() {
        LoginInfoDto user = UserHolder.getUser();
        return Result.ok(user);
    }

    @PostMapping("/logout")
    @Operation(
        summary = "用户登出",
        description = "用户登出，使Token失效"
    )
    public Result<String> logout(@Parameter(description = "custom uuid token", required = true)
            @RequestHeader("Authorization") String token) {

        authService.logout(token);
        return Result.ok("退出成功");
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0];
        }
        return request.getRemoteAddr();
    }
}