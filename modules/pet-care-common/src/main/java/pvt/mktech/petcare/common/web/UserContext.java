package pvt.mktech.petcare.common.web;

import cn.dev33.satoken.stp.StpUtil;

/**
 * {@code @description}: 用户信息上下文（支持 Servlet ThreadLocal 和 WebFlux Reactor Context）
 * {@code @date}: 2025/12/17 08:50
 *
 * @author Michael
 */
public class UserContext {

    public static final String USER_INFO_KEY = "USER_Id";

    private static final ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    // Servlet 环境使用
    public static void setUserId(Long userId) {
        threadLocal.set(userId);
    }

    public static Long getUserId() {
        // 从 ThreadLocal 获取
        Long userId = threadLocal.get();
        if (userId != null) {
            return userId;
        }
        // 从 SA-Token 获取
        try {
            if (StpUtil.isLogin()) {
                return StpUtil.getLoginIdAsLong();
            }
        } catch (Exception e) {
            // SA-Token 未初始化或其他异常，忽略
        }
        return null;
    }

    public static void removeUserId() {
        threadLocal.remove();
    }
}
