package pvt.mktech.petcare.social.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 活动查询请求DTO
 */
@Data
@Schema(description = "活动查询请求")
public class ActivityQueryRequest {

    @Schema(description = "页码", example = "1")
    private Long pageNumber = 1L;

    @Schema(description = "页大小", example = "10")
    private Long pageSize = 10L;

    @Schema(description = "状态：RECRUITING-招募中 ONGOING-进行中 ENDED-已结束")
    private String status;

    @Schema(description = "类型：ONLINE-线上活动 OFFLINE-线下聚会")
    private String activityType;
}
