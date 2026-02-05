package pvt.mktech.petcare.sync.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * {@code @description}: Activity CDC 数据（Debezium 消息体）
 * <p>包含完整的 Activity 数据，直接从 Debezium 消息中提取，无需查询数据库</p>
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Data
public class ActivityCdcData implements CdcData {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("cover_image")
    private String coverImage;

    @JsonProperty("activity_type")
    private Integer activityType;

    @JsonProperty("activity_time")
    private Long activityTime;

    @JsonProperty("end_time")
    private Long endTime;

    @JsonProperty("address")
    private String address;

    @JsonProperty("online_link")
    private String onlineLink;

    @JsonProperty("max_participants")
    private Integer maxParticipants;

    @JsonProperty("current_participants")
    private Integer currentParticipants;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("check_in_enabled")
    private Integer checkInEnabled;

    @JsonProperty("check_in_count")
    private Integer checkInCount;

    @JsonProperty("created_at")
    private Long createdAt;
}
