package pvt.mktech.petcare.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户登录、注册、登出等相关接口")
public class AuthController {
    
//    private final AuthService authService;
    
//    @PostMapping("/login")
//    @Operation(
//        summary = "用户登录",
//        description = "支持用户名、邮箱、手机号登录"
//    )
//    public ResponseEntity<LoginResponse> login(
//            @Parameter(description = "登录请求参数", required = true)
//            @Valid @RequestBody LoginRequest request,
//            HttpServletRequest httpRequest) {
//        String clientIp = getClientIp(httpRequest);
//        LoginResponse response = authService.login(request, clientIp);
//        return ResponseEntity.ok(response);
//    }
//
//    @PostMapping("/register")
//    @Operation(
//        summary = "用户注册",
//        description = "新用户注册账号"
//    )
//    public ResponseEntity<Void> register(
//            @Parameter(description = "注册请求参数", required = true)
//            @Valid @RequestBody RegisterRequest request) {
//        authService.register(request);
//        return ResponseEntity.ok().build();
//    }
//
//    @PostMapping("/logout")
//    @Operation(
//        summary = "用户登出",
//        description = "用户登出，使Token失效"
//    )
//    public ResponseEntity<Void> logout(
//            @Parameter(description = "JWT Token", required = true)
//            @RequestHeader("Authorization") String token) {
//        // 移除Bearer前缀
//        if (token.startsWith("Bearer ")) {
//            token = token.substring(7);
//        }
//        authService.logout(token);
//        return ResponseEntity.ok().build();
//    }
//
//    private String getClientIp(HttpServletRequest request) {
//        String xfHeader = request.getHeader("X-Forwarded-For");
//        if (xfHeader != null) {
//            return xfHeader.split(",")[0];
//        }
//        return request.getRemoteAddr();
//    }
}