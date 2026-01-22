package pvt.mktech.petcare.core.dto.request;

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
@Schema(description = "健康记录查询请求")
@NoArgsConstructor
@AllArgsConstructor
public class HealthRecordQueryRequest {

    @Schema(description = "宠物ID")
    private Long petId;
    @Schema(description = "记录类型")
    private String recordType;
    @Schema(description = "查询开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @Schema(description = "查询结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
