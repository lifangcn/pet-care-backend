package pvt.mktech.petcare.cdc.consumer.dto;

import lombok.Data;

import java.util.List;

/**
 * {@code @description}: ES Post 文档（聚合后，不含 embedding）
 * <p>embedding 由批量向量化任务异步生成</p>
 * {@code @date}: 2026-01-27
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
    private String locationName;
    private String locationAddress;
    private Double locationLatitude;
    private Double locationLongitude;
    private String priceRange;
    private Integer likeCount;
    private Float ratingAvg;
    private Integer viewCount;
    private Integer status;
    private Long activityId;
    private List<String> labels;

    /** 向量状态：null-未生成，[]-生成中，有值-已生成 */
    private float[] embedding;

    /** 向量生成时间戳 */
    private Long embeddedAt;

    private Long createdAt;
}
