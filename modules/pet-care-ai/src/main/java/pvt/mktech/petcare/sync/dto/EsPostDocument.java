package pvt.mktech.petcare.sync.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import pvt.mktech.petcare.sync.util.CanalInstantDeserializer;
import pvt.mktech.petcare.sync.util.JsonStringToListDeserializer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * {@code @description}: ES Post 文档（聚合后，不含 embedding）
 * <p>embedding 由批量向量化任务异步生成</p>
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Data
public class EsPostDocument {

    private Long id;

    @JsonProperty("user_id")
    private Long userId;

    private String title;
    private String content;

    @JsonProperty("post_type")
    private String postType;

    @JsonProperty("media_urls")
    @JsonDeserialize(using = JsonStringToListDeserializer.class)
    private List<String> mediaUrls;

    @JsonProperty("external_link")
    private String externalLink;

    @JsonProperty("location_address")
    private String locationAddress;

    @JsonProperty("price_range")
    private String priceRange;

    @JsonProperty("like_count")
    private Integer likeCount;

    @JsonProperty("rating_count")
    private Integer ratingCount;

    @JsonProperty("rating_total")
    private Integer ratingTotal;

    @JsonProperty("rating_avg")
    private BigDecimal ratingAvg;

    @JsonProperty("view_count")
    private Integer viewCount;

    @JsonProperty("enabled")
    private Integer enabled;

    @JsonProperty("activity_id")
    private Long activityId;

    /** 向量状态：null-未生成，[]-生成中，有值-已生成 */
    private float[] embedding;

    /** 向量生成时间戳 */
    private Long embeddedAt;

    @JsonProperty("created_at")
    @JsonDeserialize(using = CanalInstantDeserializer.class)
    private Instant createdAt;

    @JsonProperty("updated_at")
    @JsonDeserialize(using = CanalInstantDeserializer.class)
    private Instant updatedAt;

    /**
     * 是否删除（CDC 字段，不存入 ES）
     */
    @JsonProperty("is_deleted")
    private Integer isDeleted;

    /**
     * 删除时间（CDC 字段，不存入 ES）
     */
    @JsonProperty("deleted_at")
    @JsonDeserialize(using = CanalInstantDeserializer.class)
    private Instant deletedAt;
}
