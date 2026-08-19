package pvt.mktech.petcare.infrastructure.config;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.hutool.core.util.StrUtil;
import cn.hutool.jwt.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pvt.mktech.petcare.common.web.UserContext;

import java.nio.charset.StandardCharsets;

/**
 * SA-Token 配置（与 Core 模块共享 JWT 密钥）
 * 注册 SaInterceptor 以从请求头解析 JWT token，设置 UserContext
 *
 * @author Michael Li
 */
@Slf4j
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Value("${sa-token.jwt-secret-key}")
    private String jwtSecretKey;

    @Bean
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForSimple();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            HttpServletRequest request =
                    ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String token = request.getHeader("Authorization");
            if (StrUtil.isNotBlank(token) && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            if (StrUtil.isNotBlank(token)) {
                try {
                    // 使用 Hutool JWT 直接解析（SA-Token JWT 内部也用 Hutool）
                    var jwt = JWTUtil.parseToken(token);
                    if (jwt.setKey(jwtSecretKey.getBytes(StandardCharsets.UTF_8)).verify()) {
                        Object loginId = jwt.getPayload("loginId");
                        if (loginId != null) {
                            UserContext.setUserId(Long.parseLong(loginId.toString()));
                        }
                    } else {
                        log.warn("JWT 签名验证失败");
                    }
                } catch (Exception e) {
                    log.warn("JWT 解析失败: {}", e.getMessage());
                }
            }
        }))
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

        log.info("SaTokenConfig 初始化完成（AI 模块 JWT 解析）");
    }
}
