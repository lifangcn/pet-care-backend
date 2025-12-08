package pvt.mktech.petcare.context;

import pvt.mktech.petcare.dto.LoginInfoDto;

/**
 * 缓存本地
 */
public class UserHolder {
    private static final ThreadLocal<LoginInfoDto> tl = new ThreadLocal<>();

    public static void saveUser(LoginInfoDto user) {
        tl.set(user);
    }

    public static LoginInfoDto getUser() {
        return tl.get();
    }

    public static void removeUser() {
        tl.remove();
    }
}
