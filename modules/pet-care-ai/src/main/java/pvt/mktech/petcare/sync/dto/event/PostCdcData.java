package pvt.mktech.petcare.sync.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * {@code @description}: Post CDC 数据（Debezium 消息体）
 * <p>包含完整的 Post 数据，直接从 Debezium 消息中提取，无需查询数据库</p>
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Data
public class PostCdcData implements CdcData {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("content")
    private String content;

    @JsonProperty("post_type")
    private Integer postType;

    @JsonProperty("media_urls")
    private String mediaUrls;

    @JsonProperty("external_link")
    private String externalLink;

    @JsonProperty("location_address")
    private String locationAddress;

    @JsonProperty("price_range")
    private String priceRange;

    @JsonProperty("like_count")
    private Integer likeCount;

    @JsonProperty("rating_avg")
    private Float ratingAvg;

    @JsonProperty("view_count")
    private Integer viewCount;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("activity_id")
    private Long activityId;

    @JsonProperty("created_at")
    private Long createdAt;
}
