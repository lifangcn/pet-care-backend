package pvt.mktech.petcare.core.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * {@code @description}: 提醒查询请求Dto
 * {@code @date}: 2025/12/22 13:44
 *
 * @author Michael
 */
@Data
@Schema(description = "提醒查询请求")
@NoArgsConstructor
@AllArgsConstructor
public class ReminderQueryRequest {

    @Schema(description = "宠物ID")
    private Long petId;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "来源类型")
    private String sourceType;
    @Schema(description = "执行记录状态")
    private String status;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "查询开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @Schema(description = "查询结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;


    public ReminderQueryRequest(Long petId, LocalDateTime startTime, LocalDateTime endTime) {
        this.petId = petId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public ReminderQueryRequest(Long petId, String sourceType, LocalDateTime startTime, LocalDateTime endTime) {
        this.petId = petId;
        this.sourceType = sourceType;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
