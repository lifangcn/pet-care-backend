package pvt.mktech.petcare.core.dto.message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * {@code @description}: 提醒执行记录消息Dto
 * {@code @date}: 2025/12/2 15:30
 *
 * @author Michael
 */
@Schema(description = "提醒 消息Dto")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReminderMessageDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 6091337920635306143L;
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "宠物ID")
    private Long petId;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "计划执行时间")
    private LocalDateTime scheduleTime;
    @Schema(description = "提醒时间")
    private LocalDateTime nextTriggerTime;
    @Schema(description = "提前提醒时间(分钟)")
    private Integer remindBeforeMinutes;
}
