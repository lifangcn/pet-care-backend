package pvt.mktech.petcare.social.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import pvt.mktech.petcare.social.entity.Label;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * {@code @description}: 动态详情响应DTO
 * {@code @date}: 2025-01-21
 * {@code @author}: Michael
 */
@Data
@NoArgsConstructor
@Schema(description = "动态详情响应")
public class PostDetailResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "动态ID")
    private Long id;

    @Schema(description = "发布者ID")
    private Long userId;

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

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评分次数")
    private Integer ratingCount;

    @Schema(description = "评分总分")
    private Integer ratingTotal;

    @Schema(description = "平均分")
    private BigDecimal ratingAvg;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "关联的活动ID")
    private Long activityId;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "当前用户的评分值 1-5，未评分为null")
    private Integer userRatingValue;

    @Schema(description = "关联标签列表")
    private List<Label> labels;
}
