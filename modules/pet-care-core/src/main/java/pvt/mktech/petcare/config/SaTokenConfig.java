package pvt.mktech.petcare.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pvt.mktech.petcare.common.web.UserContext;

/**
 * SA-Token 配置类（整合 JWT）
 * {@code @date}: 2026/03/23
 * @author Michael Li
 */
@Slf4j
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @PostConstruct
    public void init() {
        log.info("SA-Token JWT 模式配置初始化完成");
        log.info("当前版本：{}", SaManager.getConfig());
    }

    /**
     * SA-Token 整合 JWT（Simple 简单模式）
     */
    @Bean
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForSimple();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            Long userId = resolveUserId();
            UserContext.setUserId(userId);
        }))
        .order(0)
        .addPathPatterns("/**")
        .excludePathPatterns(
            "/auth/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/doc.html",
            "/favicon.ico",
            "/webjars/**",
            "/actuator/**",
            "/error"
        );
    }

    /**
     * 解析用户ID（支持 Header 和 URL 参数两种方式）
     */
    private Long resolveUserId() {
        // SSE 场景：从 URL 参数读取 token
        String requestPath = SaHolder.getRequest().getRequestPath();
        if (requestPath.contains("sse-connect")) {
            return resolveUserIdFromUrlToken();
        }
        // 常规请求：从 Header 读取
        StpUtil.checkLogin();
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 从 URL 参数解析 token 并获取用户ID
     */
    private Long resolveUserIdFromUrlToken() {
        String token = SaHolder.getRequest().getParam("token");
        if (StrUtil.isBlank(token)) {
            throw NotLoginException.newInstance(StpUtil.getLoginType(), NotLoginException.NOT_TOKEN_MESSAGE, null, null);
        }

        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                throw NotLoginException.newInstance(StpUtil.getLoginType(), NotLoginException.NOT_TOKEN_MESSAGE, null, null);
            }
            return Long.parseLong(loginId.toString());
        } catch (NotLoginException e) {
            throw e;
        } catch (Exception e) {
            log.warn("SSE token 验证失败: {}", e.getMessage());
            throw NotLoginException.newInstance(StpUtil.getLoginType(), NotLoginException.NOT_TOKEN_MESSAGE, null, null);
        }
    }

}
