package pvt.mktech.petcare.infrastructure.config;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pvt.mktech.petcare.common.web.UserContext;

/**
 * SA-Token 配置（与 Core 模块共享 JWT 密钥）
 * 注册 SaInterceptor 以从请求头解析 JWT token，设置 UserContext
 *
 * @author Michael Li
 */
@Slf4j
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Bean
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForSimple();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public void afterCompletion(jakarta.servlet.http.HttpServletRequest request,
                                        jakarta.servlet.http.HttpServletResponse response,
                                        Object handler, Exception exception) {
                UserContext.removeUserId();
            }
        }).order(-1).addPathPatterns("/ai/**", "/admin/**");

        registry.addInterceptor(new SaInterceptor(handle -> authenticateRequest()))
        .order(0)
        .addPathPatterns("/ai/**", "/admin/**")
        .excludePathPatterns(
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/doc.html",
            "/favicon.ico",
            "/webjars/**",
            "/actuator/**",
            "/error"
        );

        log.info("SaTokenConfig 初始化完成（AI 模块强制 JWT 认证）");
    }

    void authenticateRequest() {
        StpUtil.checkLogin();
        UserContext.setUserId(StpUtil.getLoginIdAsLong());
    }
}
