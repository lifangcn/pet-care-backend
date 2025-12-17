package pvt.mktech.petcare.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "用户信息响应DTO")
public class UserResponse {

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "john_doe")
    private String username;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "昵称", example = "宠物爱好者")
    private String nickname;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatarUrl;

    @Schema(description = "性别: 0-未知 1-男 2-女", example = "1")
    private Integer gender;

    @Schema(description = "生日", example = "1990-01-01")
    private LocalDate birthday;

    @Schema(description = "用户状态: 0-禁用 1-正常 2-未激活", example = "1")
    private Integer status;

//    @Schema(description = "最后登录时间", example = "2024-01-01T10:00:00")
//    private LocalDateTime lastLoginAt;
//
//    @Schema(description = "最后登录IP", example = "192.168.1.1")
//    private String lastLoginIp;

    @Schema(description = "创建时间", example = "2024-01-01T00:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间", example = "2024-01-01T10:00:00")
    private LocalDateTime updatedAt;

    @Schema(description = "宠物数量", example = "2")
    private Integer petCount;

    @Schema(description = "默认地址ID", example = "1")
    private Long defaultAddressId;

    @Schema(description = "积分余额", example = "100")
    private Integer pointsBalance;

    @Schema(description = "会员等级", example = "1")
    private Integer memberLevel;

    @Schema(description = "会员等级名称", example = "普通会员")
    private String memberLevelName;

    public UserResponse() {
        this.petCount = 0;
        this.pointsBalance = 0;
        this.memberLevel = 1;
        this.memberLevelName = "普通会员";
    }
}