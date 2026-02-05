package pvt.mktech.petcare.sync.dto;

import lombok.Data;

import java.time.Instant;

/**
 * {@code @description}: ES Activity 文档
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Data
public class EsActivityDocument {

    private Long id;
    private Long userId;
    private String title;
    private String description;
    private Integer activityType;
    private Instant activityTime;
    private Instant endTime;
    private String address;
    private String onlineLink;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private Integer status;
    private Integer checkInEnabled;
    private Integer checkInCount;

    /** 向量状态：null-未生成，[]-生成中，有值-已生成 */
    private float[] embedding;

    /** 向量生成时间戳 */
    private Long embeddedAt;

    private Instant createdAt;
}
