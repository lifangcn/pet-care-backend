package pvt.mktech.petcare.core.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * {@code @description}:
 * {@code @date}: 2025/12/22 13:44
 *
 * @author Michael
 */
@Data
@Schema(description = "健康记录保存请求")
@NoArgsConstructor
@AllArgsConstructor
public class HealthRecordSaveRequest {

    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "宠物ID")
    private Long petId;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "记录类型")
    private String recordType;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "记录时间")
//    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordTime;
    @Schema(description = "数值(体重/体温等)")
    private BigDecimal value;
    @Schema(description = "症状信息")
    private String symptom;
    @Schema(description = "用药信息")
    private String medicationInfo;
}
