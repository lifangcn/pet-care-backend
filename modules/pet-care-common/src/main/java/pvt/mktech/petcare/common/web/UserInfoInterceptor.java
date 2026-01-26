package pvt.mktech.petcare.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import pvt.mktech.petcare.common.constant.CommonConstant;

/**
 * {@code @description} 获取用户信息拦截器
 * {@code @date} 2026-01-25
 * {@code @author} Michael
 */
@Slf4j
@Component
public class UserInfoInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 从 request attribute 中获取 JWT 拦截器解析出的 userId
        Object userIdAttr = request.getAttribute(CommonConstant.HEADER_USER_ID);
        if (userIdAttr != null) {
            try {
                Long userId = (Long) userIdAttr;
                UserContext.setUserId(userId);
                if (log.isDebugEnabled()) {
                    log.debug("用户信息设置成功: userId={}", userId);
                }
            } catch (Exception e) {
                log.warn("设置 userId 失败", e);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        UserContext.removeUserId();
    }
}
