package pvt.mktech.petcare.common.minio;

import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import pvt.mktech.petcare.common.snowflake.SnowflakeIdGenerator;

/**
 * {@code @description} : MinIO自动配置类
 * {@code @date} : 2026/1/8 19:06
 *
 * @author Michael
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(MinioProperties.class)
@ConditionalOnProperty(prefix = "minio", name = "enabled", havingValue = "true")
public class MinioAutoConfiguration {

    @Bean
    public MinioClient minioClient(MinioProperties minioProperties) {
        log.info("初始化 MinIO Client, endpoint: {}", minioProperties.getEndpoint());
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

    @Bean
    public MinioTemplate minioTemplate(MinioClient minioClient,
                                       MinioProperties minioProperties,
                                       SnowflakeIdGenerator snowflakeIdGenerator) {
        log.info("初始化 MinIO Service");
        return new MinioTemplate(minioClient, minioProperties, snowflakeIdGenerator);
    }
}
