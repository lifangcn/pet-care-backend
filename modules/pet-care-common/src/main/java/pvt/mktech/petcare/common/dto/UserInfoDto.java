package pvt.mktech.petcare.common.dto;

import lombok.Data;

/**
 * {@code @description}:
 * {@code @date}: 2025/12/17 08:54
 *
 * @author Michael
 */
@Data
public class UserInfoDto {
    private Long userId;
    private String username;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;

    public UserInfoDto(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }
}
