package pvt.mktech.petcare.common.usercache;

import pvt.mktech.petcare.common.dto.UserInfoDto;

/**
 * {@code @description}: 用户信息上下文
 * {@code @date}: 2025/12/17 08:50
 *
 * @author Michael
 */
public class UserContext {

    private static final ThreadLocal<UserInfoDto> threadLocal = new ThreadLocal<>();

    public static void setUserInfo(UserInfoDto userInfoDto) {
        threadLocal.set(userInfoDto);
    }

    public static UserInfoDto getUserInfo() {
        return threadLocal.get();
    }

    public static void removeUserInfo() {
        threadLocal.remove();
    }
}
