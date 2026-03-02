package pvt.mktech.petcare.sync.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import pvt.mktech.petcare.sync.util.CanalInstantDeserializer;

import java.time.Instant;

/**
 * {@code @description}: ES Activity 文档
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Data
public class EsActivityDocument {

    private Long id;

    @JsonProperty("user_id")
    private Long userId;

    private String title;
    private String description;

    @JsonProperty("activity_type")
    private String activityType;

    @JsonProperty("activity_time")
    @JsonDeserialize(using = CanalInstantDeserializer.class)
    private Instant activityTime;

    @JsonProperty("end_time")
    @JsonDeserialize(using = CanalInstantDeserializer.class)
    private Instant endTime;

    private String address;

    @JsonProperty("online_link")
    private String onlineLink;

    @JsonProperty("max_participants")
    private Integer maxParticipants;

    @JsonProperty("current_participants")
    private Integer currentParticipants;

    private String status;

    @JsonProperty("check_in_enabled")
    private Integer checkInEnabled;

    @JsonProperty("check_in_count")
    private Integer checkInCount;

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
