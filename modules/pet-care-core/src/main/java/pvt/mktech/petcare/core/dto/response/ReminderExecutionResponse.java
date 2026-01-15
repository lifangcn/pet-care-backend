package pvt.mktech.petcare.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * {@code @description}: 提醒事项响应DTO
 * {@code @date}: 2026/1/14 13:51
 *
 * @author Michael
 */
@Schema(description = "提醒事项响应DTO")
@Data
public class ReminderExecutionResponse {
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "提醒ID")
    private Long reminderId;
    @Schema(description = "宠物ID")
    private Long petId;
    @Schema(description = "计划执行时间")
    private LocalDateTime scheduleTime;
    @Schema(description = "实际执行时间")
    private LocalDateTime actualTime;
    @Schema(description = "执行状态")
    private String status;
    @Schema(description = "完成说明")
    private String completionNotes;
    @Schema(description = "通知时间")
    private LocalDateTime notificationTime;
    @Schema(description = "是否已读")
    private Boolean isRead;
    @Schema(description = "是否已发送")
    private Boolean isSent;
    @Schema(description = "发送时间")
    private LocalDateTime sentAt;
    @Schema(description = "阅读时间")
    private LocalDateTime readAt;

    @Schema(description = "提醒标题")
    private String reminderTitle;
    @Schema(description = "提醒描述")
    private String reminderDescription;
    @Schema(description = "宠物名称")
    private String petName;
    @Schema(description = "宠物名称")
    private String petAvatar;
}
