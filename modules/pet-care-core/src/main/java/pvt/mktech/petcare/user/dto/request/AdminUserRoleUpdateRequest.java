package pvt.mktech.petcare.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新用户管理员角色请求DTO
 *
 * @author Michael Li
 * @since 2026-03-27
 */
@Data
@Schema(description = "更新用户管理员角色请求DTO")
public class AdminUserRoleUpdateRequest {

    @Schema(description = "是否授予管理员权限: true-授予 false-移除", required = true, example = "true")
    @NotNull(message = "isAdmin不能为空")
    private Boolean isAdmin;
}
