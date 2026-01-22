package pvt.mktech.petcare.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * {@code @description}: 用户打卡统计响应DTO
 * {@code @date}: 2026/1/14 13:51
 *
 * @author Michael
 */
@Data
@Schema(description = "用户打卡统计响应DTO")
public class CheckInStatsResponse {
    @Schema(description = "打卡次数", example = "15")
    private Integer monthCheckInCount;
    @Schema(description = "连续打开天数", example = "5")
    private Integer continuousDays;
    @Schema(description = "最后一次打卡日期", example = "2025-01-15")
    private String lastCheckInDate;
    @Schema(description = "打卡记录", example = "['2025-01-15', '2025-01-16', '2025-01-18']")
    private List<String> checkInDates;
}
