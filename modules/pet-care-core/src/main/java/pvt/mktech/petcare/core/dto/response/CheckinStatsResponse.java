package pvt.mktech.petcare.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * {@code @description}: 用户打卡统计响应DTO
 * {@code @date}: 2026/1/14 13:51
 *
 * @author Michael
 */
@Schema(description = "用户打卡统计响应DTO")
@Data
public class CheckinStatsResponse {
    @Schema(description = "打卡次数", example = "15")
    private Integer monthCheckinCount;
    @Schema(description = "连续打开天数", example = "5")
    private Integer continuousDays;
    @Schema(description = "最后一次打卡日期", example = "2025-01-15")
    private String lastCheckinDate;


}
