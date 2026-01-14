package pvt.mktech.petcare.common.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code @description}: MinIO 配置属性
 * {@code @date}: 2026/1/8 19:24
 *
 * @author Michael
 */
@Data
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    private Boolean enabled = false;
    private String businessKey;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private Long fileSizeLimit;
    private Long expireTime;
}
