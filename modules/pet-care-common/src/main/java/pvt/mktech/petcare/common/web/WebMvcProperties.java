package pvt.mktech.petcare.common.web;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code @description} Web MVC 拦截器路径配置
 * {@code @date} 2026-01-25
 * {@code @author} Michael
 */
@Data
@ConfigurationProperties(prefix = "web.mvc")
public class WebMvcProperties {

    /**
     * JWT 认证拦截器路径
     */
    private List<String> jwtIncludePaths = new ArrayList<>();

    /**
     * JWT 认证拦截器排除路径
     */
    private List<String> jwtExcludePaths = List.of("/auth/**", "/actuator/**", "/error");
}
