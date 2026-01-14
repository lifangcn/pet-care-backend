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
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssProperties {
    private Boolean enabled = false;
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
}
