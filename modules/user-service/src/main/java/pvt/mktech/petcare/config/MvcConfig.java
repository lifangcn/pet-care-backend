package pvt.mktech.petcare.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pvt.mktech.petcare.interceptor.LoginInterceptor;
import pvt.mktech.petcare.interceptor.RefreshTokenInterceptor;

/**
 * {@code @description}:
 * {@code @date}: 2025/11/28 15:35
 *
 * @author Michael
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Token刷新拦截器
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate)).addPathPatterns("/**").order(0);
//        // 登录拦截器
        registry.addInterceptor(new LoginInterceptor()).excludePathPatterns(
                "/v1/auth/code",
                "/v1/auth/login",
                "/voucher/**",
                "/upload/**",
                "/blog/hot"
        ).order(1);
    }
}
