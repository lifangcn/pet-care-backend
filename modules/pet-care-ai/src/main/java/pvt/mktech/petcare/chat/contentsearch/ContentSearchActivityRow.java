package pvt.mktech.petcare.chat.contentsearch;

import lombok.Data;
import java.time.LocalDateTime;

/** Typed PostgreSQL activity search row. */
@Data
public class ContentSearchActivityRow {
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String address;
    private LocalDateTime activityTime;
    private String status;
    private LocalDateTime createdAt;
    private Double score;
}
