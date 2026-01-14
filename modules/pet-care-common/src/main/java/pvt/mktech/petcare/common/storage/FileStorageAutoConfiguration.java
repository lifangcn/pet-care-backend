package pvt.mktech.petcare.common.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
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
@EnableConfigurationProperties({MinioProperties.class, OssProperties.class})
public class FileStorageAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "minio", name = "enabled", havingValue = "true")
    public MinioClient minioClient(MinioProperties minioProperties) {
        log.info("初始化 MinIO Client, endpoint: {}", minioProperties.getEndpoint());
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "minio", name = "enabled", havingValue = "true")
    public MinioTemplate minioTemplate(MinioClient minioClient,
                                       MinioProperties minioProperties,
                                       SnowflakeIdGenerator snowflakeIdGenerator) {
        log.info("初始化 MinIO Template");
        return new MinioTemplate(minioClient, minioProperties, snowflakeIdGenerator);
    }

    @Bean
    @ConditionalOnProperty(prefix = "aliyun.oss", name = "enabled", havingValue = "true")
    public OSS ossClient(OssProperties ossProperties) {
        log.info("初始化 Aliyun OSS Client, endpoint: {}", ossProperties.getEndpoint());
        return new OSSClientBuilder().build(ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret());
    }

    @Bean
    @ConditionalOnProperty(prefix = "aliyun.oss", name = "enabled", havingValue = "true")
    public OssTemplate ossTemplate(OSS oss, OssProperties ossProperties, SnowflakeIdGenerator snowflakeIdGenerator) {
        log.info("初始化 Aliyun OSS Template, endpoint: {}", ossProperties.getEndpoint());
        return new OssTemplate(oss, ossProperties, snowflakeIdGenerator);
    }
}
