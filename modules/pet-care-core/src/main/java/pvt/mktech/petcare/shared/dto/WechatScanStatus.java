package pvt.mktech.petcare.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pvt.mktech.petcare.user.dto.LoginInfoDto;

/**
 * {@code @description}: 微信扫码状态响应
 * {@code @date}: 2025/01/16
 *
 * @author Michael
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "微信扫码状态响应")
public class WechatScanStatus {

    @Schema(description = "扫码状态：WAITING-等待扫码，SCANNED-已扫码待确认，CONFIRMED-已确认登录")
    private String status;

    @Schema(description = "登录信息，状态为CONFIRMED时返回")
    private LoginInfoDto loginInfo;
}
