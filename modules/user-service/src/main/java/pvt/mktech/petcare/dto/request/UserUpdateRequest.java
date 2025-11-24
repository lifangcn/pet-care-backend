package pvt.mktech.petcare.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "用户更新请求DTO")
public class UserUpdateRequest {
    
    @Schema(description = "昵称", example = "宠物爱好者")
    @Size(min = 1, max = 50, message = "昵称长度必须在1-50个字符之间")
    private String nickname;
    
    @Schema(description = "邮箱", example = "user@example.com")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;
    
    @Schema(description = "手机号", example = "13800138000")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    @Size(max = 500, message = "头像URL长度不能超过500个字符")
    private String avatarUrl;
    
    @Schema(description = "性别: 0-未知 1-男 2-女", example = "1")
    private Integer gender;
    
    @Schema(description = "生日", example = "1990-01-01")
    private LocalDate birthday;
    
    @Schema(description = "个人简介", example = "我是一个热爱宠物的人")
    @Size(max = 500, message = "个人简介长度不能超过500个字符")
    private String bio;
    
    @Schema(description = "所在城市", example = "北京市")
    @Size(max = 100, message = "城市名称长度不能超过100个字符")
    private String city;
    
    @Schema(description = "职业", example = "软件工程师")
    @Size(max = 100, message = "职业长度不能超过100个字符")
    private String occupation;
    
    @Schema(description = "养宠经验(年)", example = "3")
    private Integer petExperience;
    
    public UserUpdateRequest() {
        this.gender = 0; // 默认未知
        this.petExperience = 0;
    }
    
    /**
     * 验证更新数据的有效性
     */
    public boolean isValidUpdate() {
        return nickname != null || email != null || phone != null || 
               avatarUrl != null || gender != null || birthday != null ||
               bio != null || city != null || occupation != null || 
               petExperience != null;
    }
}