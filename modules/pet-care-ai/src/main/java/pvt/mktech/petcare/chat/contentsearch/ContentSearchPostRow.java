package pvt.mktech.petcare.chat.contentsearch;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Typed PostgreSQL post search row. */
@Data
public class ContentSearchPostRow {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String postType;
    private String locationAddress;
    private String priceRange;
    private Integer likeCount;
    private BigDecimal ratingAvg;
    private LocalDateTime createdAt;
    private Double score;
}
