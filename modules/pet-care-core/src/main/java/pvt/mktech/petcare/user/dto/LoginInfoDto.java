package pvt.mktech.petcare.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * {@code @description}:
 * {@code @date}: 2025/12/2 15:30
 *
 * @author Michael
 */
@Data
@Schema(description = "登录信息DTO")
public class LoginInfoDto {

    @JsonIgnore
    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "访问Token")
    private String accessToken;

    @Schema(description = "刷新Token")
    private String refreshToken;

    @Schema(description = "过期时间戳")
    private Long expiresIn;
}
