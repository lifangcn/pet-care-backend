package pvt.mktech.petcare.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SpringDoc OpenAPI 3 配置类
 * 替代原有的 SpringFox Swagger 配置
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:user-service}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // API 信息
                .info(new Info()
                        .title("宠物关怀系统 - 用户服务 API")
                        .description("""
                                用户管理、宠物档案管理、认证授权相关接口
                                
                                ## 功能模块
                                - 用户认证与授权
                                - 用户信息管理  
                                - 宠物档案管理
                                - 地址管理
                                - 健康记录管理
                                
                                ## 技术栈
                                - Spring Boot 3.5.6
                                - MyBatis-Flex
                                - RocketMQ
                                - SpringDoc OpenAPI 3
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("PetCare Team")
                                .email("contact@petcare.com")
                                .url("https://petcare.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0.html")))
                // 服务器配置
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("开发环境"),
                        new Server()
                                .url("https://api.petcare.com")
                                .description("生产环境")
                ))
                // 安全配置 - JWT
                /*.addSecurityItem(new SecurityRequirement().addList("JWT"))
                .components(new Components()
                        .addSecuritySchemes("JWT", new SecurityScheme()
                                .name("JWT")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("请输入JWT Token，格式: Bearer {token}")))*/;
    }
}