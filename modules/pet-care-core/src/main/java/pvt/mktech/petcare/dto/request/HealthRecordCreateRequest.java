package pvt.mktech.petcare.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "健康记录创建请求")
public class HealthRecordCreateRequest {

    @NotNull(message = "记录类型不能为空")
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