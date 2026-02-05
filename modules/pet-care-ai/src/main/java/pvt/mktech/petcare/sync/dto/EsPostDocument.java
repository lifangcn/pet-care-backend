package pvt.mktech.petcare.sync.dto;

import lombok.Data;

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
    private Long userId;
    private String title;
    private String content;
    private Integer postType;
    private List<String> mediaUrls;
    private String externalLink;
    private String locationAddress;
    private String priceRange;
    private Integer likeCount;
    private Float ratingAvg;
    private Integer viewCount;
    private Integer status;
    private Long activityId;

    /** 向量状态：null-未生成，[]-生成中，有值-已生成 */
    private float[] embedding;

    /** 向量生成时间戳 */
    private Long embeddedAt;

    private Instant createdAt;
}
