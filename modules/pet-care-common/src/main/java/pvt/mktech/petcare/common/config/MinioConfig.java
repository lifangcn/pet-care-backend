package pvt.mktech.petcare.common.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code @description}: Minio配置类
 * {@code @date}: 2025/12/17 16:17
 *
 * @author Michael
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
@ConditionalOnProperty(prefix = "minio", value = "enable", havingValue = "true")
public class MinioConfig {
    private Boolean enable;
    private String businessKey;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private Long fileSizeLimit;
    private Long expireTime;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
