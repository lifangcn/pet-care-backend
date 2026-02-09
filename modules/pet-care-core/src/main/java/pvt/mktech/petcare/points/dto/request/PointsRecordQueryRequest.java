package pvt.mktech.petcare.points.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * {@code @description}: 积分流水查询请求
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
@Data
@Schema(description = "积分流水查询请求")
@NoArgsConstructor
@AllArgsConstructor
public class PointsRecordQueryRequest {

    @Schema(description = "行为类型")
    private Integer actionType;

    @Schema(description = "查询开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "查询结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
