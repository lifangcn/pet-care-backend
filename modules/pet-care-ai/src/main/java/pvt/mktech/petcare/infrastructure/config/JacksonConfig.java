package pvt.mktech.petcare.infrastructure.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code @description}: Jackson 配置
 * <p>支持 Canal Flat Message 格式：字符串转数字</p>
 * <p>支持 LocalDateTime 序列化/反序列化</p>
 * {@code @date}: 2026-02-05
 * @author Michael Li
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                // 允许字符串转数字（Canal Flat Message 格式要求）
                .enable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                // 忽略JSON中存在但Java类中不存在的字段（兼容第三方API响应格式变更）
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // 注册 JavaTimeModule 支持 LocalDateTime
                .addModule(new JavaTimeModule())
                .build();
    }
}
