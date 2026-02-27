package pvt.mktech.petcare.sync.entity.core;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * {@code @description}: Post 表实体（仅查询用）
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Data
@Table(value = "tb_post", dataSource = "core")
public class PostEntity {

    @Id
    private Long id;

    private Long userId;
    private String title;
    private String content;
    private String postType;
    private String mediaUrls;
    private String externalLink;
    private String priceRange;
    private Integer likeCount;
    private BigDecimal ratingAvg;
    private Integer viewCount;
    private String status;
    private Long activityId;
    private LocalDateTime createdAt;
}
