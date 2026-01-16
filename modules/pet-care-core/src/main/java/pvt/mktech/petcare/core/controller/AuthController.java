package pvt.mktech.petcare.core.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.core.dto.LoginInfoDto;
import pvt.mktech.petcare.core.dto.request.LoginRequest;
import pvt.mktech.petcare.core.dto.response.WechatQRCodeResponse;
import pvt.mktech.petcare.core.dto.response.WechatScanStatus;
import pvt.mktech.petcare.core.handler.ReminderWebSocketHandler;
import pvt.mktech.petcare.core.service.AuthService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户登录、注册、登出等相关接口")
public class AuthController {

    private final AuthService authService;
    private final ReminderWebSocketHandler reminderWebSocketHandler;

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
            @Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "用户刷新操作",
            description = "用户刷新操作"
    )
    public Result<LoginInfoDto> refresh(@RequestBody LoginInfoDto dto) {
        LoginInfoDto refreshedDto = authService.refreshToken(dto);
        return Result.success(refreshedDto);
    }

    @PostMapping("/logout")
    @Operation(
            summary = "用户登出",
            description = "用户登出，使Token失效"
    )
    public Result<String> logout(@RequestBody(required = false) LoginInfoDto dto) {
        // TODO 退出登录报错
        authService.logout(dto);
        return Result.success("登出成功");
    }

    @PostMapping("/wechat/qrcode")
    @Operation(
            summary = "获取微信登录二维码",
            description = "生成微信登录二维码，前端轮询/checkWechatScanStatus检查扫码状态"
    )
    public Result<WechatQRCodeResponse> getWechatQRCode() {
        return authService.getWechatQRCode();
    }

    @GetMapping("/wechat/scan-status")
    @Operation(
            summary = "检查微信扫码状态",
            description = "轮询接口，返回扫码状态：WAITING-等待扫码，SCANNED-已扫码待确认，CONFIRMED-已确认登录"
    )
    public Result<WechatScanStatus> checkWechatScanStatus(
            @Parameter(description = "二维码ticket", required = true)
            @RequestParam("ticket") String ticket) {
        return authService.checkWechatScanStatus(ticket);
    }
}