package pvt.mktech.petcare.club.dto.request;

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

    @Schema(description = "活动状态 1-招募中 2-进行中 3-已结束")
    private Integer status;

    @Schema(description = "活动类型 1-线上活动 2-线下聚会")
    private Integer activityType;
}
