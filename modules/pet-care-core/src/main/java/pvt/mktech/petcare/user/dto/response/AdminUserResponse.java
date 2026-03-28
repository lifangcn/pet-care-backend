package pvt.mktech.petcare.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员用户列表响应DTO
 *
 * @author Michael Li
 * @since 2026-03-27
 */
@Data
@Schema(description = "管理员用户列表响应DTO")
public class AdminUserResponse {

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "john_doe")
    private String username;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "昵称", example = "宠物爱好者")
    private String nickname;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "是否启用: 0-禁用 1-启用", example = "1")
    private Integer enabled;

    @Schema(description = "是否是管理员", example = "true")
    private Boolean isAdmin;

    @Schema(description = "创建时间", example = "2024-01-01T00:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间", example = "2024-01-01T10:00:00")
    private LocalDateTime updatedAt;
}
