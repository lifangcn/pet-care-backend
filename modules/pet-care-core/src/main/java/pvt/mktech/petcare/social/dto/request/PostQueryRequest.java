package pvt.mktech.petcare.social.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 动态查询请求DTO
 */
@Data
@Schema(description = "动态查询请求")
public class PostQueryRequest {

    @Schema(description = "页码", example = "1")
    private Long pageNumber = 1L;

    @Schema(description = "页大小", example = "10")
    private Long pageSize = 10L;

    @Schema(description = "类型：PRODUCT-好物分享 SERVICE-服务推荐 LOCATION-地点推荐 DAILY-日常分享 ACTIVITY_CHECK-活动打卡 ACTIVITY_JOIN-活动报名")
    private String postType;

    @Schema(description = "标签ID列表")
    private String tagIds;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "排序方式 latest-最新 hottest-最热 rating-评分最高", example = "latest")
    private String sortBy = "latest";

    @Schema(description = "活动ID")
    private Long activityId;
}
