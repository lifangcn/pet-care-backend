package pvt.mktech.petcare.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
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
@Slf4j
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 登录拦截器
        registry.addInterceptor(new UserInfoInterceptor()).excludePathPatterns("/actuator/**").order(0);
        log.info("添加登录拦截器成功");
    }
}
