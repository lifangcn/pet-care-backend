package pvt.mktech.petcare.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import pvt.mktech.petcare.common.util.JwtUtil;
import pvt.mktech.petcare.context.UserHolder;
import pvt.mktech.petcare.dto.LoginInfoDto;

import java.time.Duration;
import java.util.Map;

import static pvt.mktech.petcare.context.UserConstants.LOGIN_TOKEN_KEY;
import static pvt.mktech.petcare.context.UserConstants.LOGIN_TOKEN_TTL;

/**
 * {@code @description}: 更新Token拦截器
 * {@code @date}: 2025/11/28 15:44
 *
 * @author Michael
 */
public record RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token)) {
            return true;
        }
        try {
            if (JwtUtil.isTokenExpired(token)) {
                return true;
            }
            String key = LOGIN_TOKEN_KEY + token;
            Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
            if (userMap.isEmpty()) {
                return true;
            }
            LoginInfoDto loginInfoDto = BeanUtil.fillBeanWithMap(userMap, new LoginInfoDto(), false);
            UserHolder.saveUser(loginInfoDto);
            stringRedisTemplate.expire(key, Duration.ofSeconds(LOGIN_TOKEN_TTL));
        } catch (Exception e) {
            return true;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
