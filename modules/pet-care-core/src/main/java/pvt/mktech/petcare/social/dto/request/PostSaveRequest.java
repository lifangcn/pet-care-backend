package pvt.mktech.petcare.social.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * {@code @description}: 动态发布请求DTO
 * {@code @date}: 2025-01-22
 * {@code @author}: Michael
 */
@Data
@Schema(description = "动态发布请求")
public class PostSaveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容描述")
    private String content;

    @Schema(description = "类型：PRODUCT-好物分享 SERVICE-服务推荐 LOCATION-地点推荐 DAILY-日常分享 ACTIVITY_CHECK-活动打卡 ACTIVITY_JOIN-活动报名")
    private String postType;

    @Schema(description = "图片/视频URL数组")
    private List<String> mediaUrls;

    @Schema(description = "外部链接（商品、服务、地图）")
    private String externalLink;

    @Schema(description = "地点地址")
    private String locationAddress;

    @Schema(description = "价格区间")
    private String priceRange;

    @Schema(description = "关联的活动ID")
    private Long activityId;

    @Schema(description = "标签ID列表")
    private List<Long> labelIds;
}
