package pvt.mktech.petcare.sync.entity.core;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * {@code @description}: Activity 表实体（仅查询用）
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Data
@Table(value = "tb_activity", dataSource = "core")
public class ActivityEntity {

    @Id
    private Long id;

    private Long userId;
    private String title;
    private String description;
    private String coverImage;
    private Integer activityType;
    private LocalDateTime activityTime;
    private LocalDateTime endTime;
    private String address;
    private String onlineLink;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private Integer status;
    private Integer checkInEnabled;
    private Integer checkInCount;
    private LocalDateTime createdAt;
}
