package pvt.mktech.petcare.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pvt.mktech.petcare.common.interceptor.UserInfoInterceptor;

/**
 * {@code @description}: 拦截器配置类
 * {@code @date}: 2025/11/28 15:35
 *
 * @author Michael
 */
@Configuration
@ConditionalOnClass(WebMvcConfigurer.class)
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 登录拦截器
        registry.addInterceptor(new UserInfoInterceptor()).order(0);
    }
}
