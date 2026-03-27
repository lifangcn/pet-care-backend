package pvt.mktech.petcare.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 自动配置
 * 注意：认证拦截器已由 Sa-Token 统一处理，此处仅处理用户上下文清理
 * {@code @date} 2026-01-25
 * {@code @author} Michael
 */
@Slf4j
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ComponentScan(basePackages = "pvt.mktech.petcare.common.web")
public class WebMvcAutoConfiguration implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 用户上下文清理拦截器（请求结束后清理 ThreadLocal）
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                        Object handler, Exception ex) {
                UserContext.removeUserId();
            }
        }).order(Integer.MAX_VALUE);

        log.info("WebMvcAutoConfiguration 初始化完成（Sa-Token 统一认证模式）");
    }
}
