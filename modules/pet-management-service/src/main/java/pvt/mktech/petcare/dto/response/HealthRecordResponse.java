package pvt.mktech.petcare.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "健康记录响应")
public class HealthRecordResponse {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "宠物ID")
    private Long petId;

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

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}