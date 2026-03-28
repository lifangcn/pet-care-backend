package pvt.mktech.petcare.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新用户启用状态请求DTO
 *
 * @author Michael Li
 * @since 2026-03-27
 */
@Data
@Schema(description = "更新用户启用状态请求DTO")
public class AdminUserEnabledUpdateRequest {

    @Schema(description = "是否启用: 0-禁用 1-启用", required = true, example = "1")
    @NotNull(message = "enabled不能为空")
    private Integer enabled;
}
