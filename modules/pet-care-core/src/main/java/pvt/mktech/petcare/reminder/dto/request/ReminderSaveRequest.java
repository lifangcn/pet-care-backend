package pvt.mktech.petcare.reminder.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * {@code @description}:
 * {@code @date}: 2025/12/22 13:44
 *
 * @author Michael
 */
@Data
@Schema(description = "提醒保存请求DTO")
@NoArgsConstructor
@AllArgsConstructor
public class ReminderSaveRequest {

    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "宠物ID")
    private Long petId;
    @Schema(description = "宠物名称")
    private String petName;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "来源类型：'manual' | 'health_record' | 'system'")
    private String sourceType;
    @Schema(description = "来源ID，通过健康记录创建，则记录 healthRecord.id")
    private Long sourceId;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "记录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordTime;
    @Schema(description = "计划执行时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduleTime;
    @Schema(description = "提前提醒时间(分钟)")
    private Integer remindBeforeMinutes;
    @Schema(description = "重复类型：'NONE' | 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'CUSTOM'")
    private String repeatType;
    @Schema(description = "重复配置")
    private String repeatConfig;
    @Schema(description = "是否激活")
    private Boolean isActive = Boolean.TRUE;
    @Schema(description = "总执行次数")
    private Integer totalOccurrences;
    @Schema(description = "已完成次数")
    private Integer completedCount;
    @Schema(description = "完成时间")
    private LocalDateTime completedTime;
}
