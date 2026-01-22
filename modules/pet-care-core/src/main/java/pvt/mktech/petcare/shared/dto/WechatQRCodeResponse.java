package pvt.mktech.petcare.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * {@code @description}: 微信二维码响应
 * {@code @date}: 2025/01/16
 *
 * @author Michael
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "微信二维码响应")
public class WechatQRCodeResponse {

    @Schema(description = "二维码图片URL")
    private String qrcodeUrl;

    @Schema(description = "二维码票据，用于轮询扫码状态")
    private String ticket;

    @Schema(description = "过期时间（秒）")
    private Long expireTime;
}
