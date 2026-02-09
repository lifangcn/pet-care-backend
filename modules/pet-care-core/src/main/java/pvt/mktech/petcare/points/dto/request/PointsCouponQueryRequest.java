package pvt.mktech.petcare.points.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * {@code @description}: 积分代金券查询请求
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
@Data
@Schema(description = "积分代金券查询请求")
@NoArgsConstructor
@AllArgsConstructor
public class PointsCouponQueryRequest {

    @Schema(description = "券状态：0-未使用 1-已使用 2-已过期")
    private Integer status;
}
