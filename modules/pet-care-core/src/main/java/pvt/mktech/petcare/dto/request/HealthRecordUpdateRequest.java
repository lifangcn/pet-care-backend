package pvt.mktech.petcare.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "健康记录更新请求")
public class HealthRecordUpdateRequest {

    @Schema(description = "记录类型: 1-体重 2-体温 3-症状 4-用药 5-其他")
    private Integer recordType;

    @Schema(description = "记录数值")
    private Double value;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "备注")
    private String notes;

    @Schema(description = "记录时间")
    private LocalDateTime recordDate;
}