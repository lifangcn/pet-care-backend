package pvt.mktech.petcare.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "登录请求")
public class LoginRequest {
    @NotBlank(message = "手机号不能为空")
    @Schema(description = "手机号", example = "15971234567")
    private String phone;
    @Schema(description = "验证码", example = "123321")
    @Pattern(regexp = "^\\d{6}$", message = "验证码格式不正确")
    private String code;
}