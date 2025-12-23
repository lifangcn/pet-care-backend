package pvt.mktech.petcare.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * {@code @description}: 提醒查询请求Dto
 * {@code @date}: 2025/12/22 13:44
 *
 * @author Michael
 */
@Data
@Schema(description = "提醒查询请求")
public class ReminderQueryRequest {

    @Schema(description = "宠物ID")
    private Long petId;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "来源类型")
    private String sourceType;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "记录时间开始")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordTimeStart;
    @Schema(description = "记录时间结束")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordTimeEnd;
    @Schema(description = "页码")
    private Long pageNumber = 1L;
    @Schema(description = "每页数量")
    private Long pageSize;

    public ReminderQueryRequest(Long petId, String sourceType, LocalDateTime recordTimeStart, LocalDateTime recordTimeEnd, Long pageNumber, Long pageSize) {
        this.petId = petId;
        this.sourceType = sourceType;
        this.recordTimeStart = recordTimeStart;
        this.recordTimeEnd = recordTimeEnd;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }
}
