package pvt.mktech.petcare.common.usercache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * {@code @description}: 拦截器配置类
 * {@code @date}: 2025/11/28 15:35
 *
 * @author Michael
 */
@Slf4j
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Order(0)
public class UserCacheConfiguration implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 登录拦截器
        registry.addInterceptor(new UserInfoInterceptor())
                .excludePathPatterns(
                        "/actuator/**",           // 监控端点
                        "/error",                 // 错误页面
                        "/swagger-ui/**",         // Swagger UI
                        "/doc.html",              // Knife4j 文档
                        "/webjars/**",            // WebJars 资源
                        "/static/**", "/css/**", "/js/**", "/images/**" // 静态资源
                );
        log.info("添加登录拦截器成功");
    }
}
