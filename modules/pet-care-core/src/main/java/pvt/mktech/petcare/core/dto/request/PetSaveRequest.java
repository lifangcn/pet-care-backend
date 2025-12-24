package pvt.mktech.petcare.core.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Date;

/**
 * {@code @description}:
 * {@code @date}: 2025/12/22 13:44
 *
 * @author Michael
 */
@Data
@Schema(description = "宠物信息保存请求")
@NoArgsConstructor
@AllArgsConstructor
public class PetSaveRequest {

    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "宠物名称")
    private String name;
    @Schema(description = "宠物类型")
    private String type;
    @Schema(description = "种类")
    private String breed;
    @Schema(description = "性别")
    private Boolean gender;
    @Schema(description = "出生日期")
    private Date birthday;
    @Schema(description = "体重")
    private BigDecimal weight;
    @Schema(description = "头像")
    private String avatar;
}
