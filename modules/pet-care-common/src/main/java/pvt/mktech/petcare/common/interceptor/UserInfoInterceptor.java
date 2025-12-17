package pvt.mktech.petcare.common.interceptor;

import cn.hutool.core.util.StrUtil;
import io.jsonwebtoken.Header;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import pvt.mktech.petcare.common.constant.CommonConstant;
import pvt.mktech.petcare.common.context.UserContext;
import pvt.mktech.petcare.common.dto.UserInfoDto;

/**
 * {@code @description}:
 * {@code @date}: 2025/12/17 08:46
 *
 * @author Michael
 */
public class UserInfoInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userId = request.getHeader(CommonConstant.HEADER_USER_ID);
        String username = request.getHeader(CommonConstant.HEADER_USERNAME);
        if (StrUtil.isNotBlank(userId) && StrUtil.isNotBlank(username)) {
            UserContext.setUserInfo(new UserInfoDto(Long.parseLong(userId), username));
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.removeUserInfo();
    }
}
