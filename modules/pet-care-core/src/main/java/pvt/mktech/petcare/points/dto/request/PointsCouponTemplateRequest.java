package pvt.mktech.petcare.points.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 积分券模板创建/更新请求
 *
 * @author Michael Li
 * @since 2026-03-27
 */
@Data
@Schema(description = "积分券模板创建/更新请求")
@NoArgsConstructor
@AllArgsConstructor
public class PointsCouponTemplateRequest {

    @Schema(description = "券名称")
    @NotBlank(message = "券名称不能为空")
    private String name;

    @Schema(description = "面值（可抵扣积分数）")
    @NotNull(message = "面值不能为空")
    @Min(value = 1, message = "面值必须大于0")
    private Integer faceValue;

    @Schema(description = "有效天数（领取后计算）")
    @NotNull(message = "有效天数不能为空")
    @Min(value = 1, message = "有效天数必须大于0")
    private Integer validDays;

    @Schema(description = "发放总量（0表示不限）")
    @NotNull(message = "发放总量不能为空")
    @Min(value = 0, message = "发放总量不能为负数")
    private Integer totalCount;

    @Schema(description = "每人限领数量")
    @NotNull(message = "每人限领数量不能为空")
    @Min(value = 1, message = "每人限领数量必须大于0")
    private Integer perUserLimit;

    @Schema(description = "来源类型：SYSTEM-系统发放 ACTIVITY-活动发放 NEWCOMER-新人礼包")
    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    @Schema(description = "状态：0-停用 1-启用")
    @NotNull(message = "状态不能为空")
    private Integer status;
}
