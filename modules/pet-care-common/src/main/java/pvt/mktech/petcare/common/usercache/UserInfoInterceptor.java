package pvt.mktech.petcare.common.usercache;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;
import pvt.mktech.petcare.common.constant.CommonConstant;
import pvt.mktech.petcare.common.dto.UserInfoDto;

/**
 * {@code @description}: 获取用户信息拦截器
 * {@code @date}: 2025/12/17 08:46
 *
 * @author Michael
 */
@Slf4j
public class UserInfoInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String userId = request.getHeader(CommonConstant.HEADER_USER_ID);
        if (StrUtil.isNotBlank(userId)) {
            try {
                UserContext.setUserId(Long.parseLong(userId));
                if (log.isDebugEnabled()) {
                    log.debug("用户信息设置成功: userId={}", userId);
                }
            } catch (NumberFormatException e) {
                // userId 格式错误，记录日志但不影响请求继续
                log.warn("无效的 userId 格式: {}", userId);
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
