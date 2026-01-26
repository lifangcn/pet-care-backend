package pvt.mktech.petcare.common.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * {@code @description} Web MVC 自动配置，统一管理拦截器
 * {@code @date} 2026-01-25
 * {@code @author} Michael
 */
@Slf4j
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@RequiredArgsConstructor
@EnableConfigurationProperties(WebMvcProperties.class)
@ComponentScan(basePackages = "pvt.mktech.petcare.common.web")
public class WebMvcAutoConfiguration implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final UserInfoInterceptor userInfoInterceptor;
    private final WebMvcProperties webMvcProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // JWT 认证拦截器（优先级高，先执行）
        if (!webMvcProperties.getJwtIncludePaths().isEmpty()) {
            registry.addInterceptor(jwtAuthInterceptor)
                    .order(1)
                    .addPathPatterns(webMvcProperties.getJwtIncludePaths())
                    .excludePathPatterns(webMvcProperties.getJwtExcludePaths());
            log.info("添加 JWT 认证拦截器成功，包含路径: {}, 排除路径: {}",
                    webMvcProperties.getJwtIncludePaths(), webMvcProperties.getJwtExcludePaths());
        }

        // 用户信息拦截器（从 request attribute 获取 userId 并缓存到 ThreadLocal）
        registry.addInterceptor(userInfoInterceptor)
                .order(2)
                .excludePathPatterns(
                        "/actuator/**",
                        "/error",
                        "/swagger-ui/**",
                        "/doc.html",
                        "/webjars/**",
                        "/static/**", "/css/**", "/js/**", "/images/**"
                );
        log.info("添加用户信息拦截器成功");
    }
}
