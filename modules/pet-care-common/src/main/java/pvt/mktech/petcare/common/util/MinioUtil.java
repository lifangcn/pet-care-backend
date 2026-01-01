package pvt.mktech.petcare.common.util;

import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.util.StrUtil;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import pvt.mktech.petcare.common.config.MinioConfig;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.exception.SystemException;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code @description}:
 * {@code @date}: 2025/12/17 16:22
 *
 * @author Michael
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(MinioClient.class)
public class MinioUtil {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    public static final String MINIO_POLICY_AVATARS = """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Principal": "*",
                  "Action": [
                    "s3:GetObject"
                  ],
                  "Resource": [
                    "arn:aws:s3:::%s/avatars/*"
                  ]
                }
              ]
            }
            """;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final DistributedIdGenerator idGenerator;

    // 允许的头像文件类型
    private static final Map<String, String> ALLOWED_IMAGE_TYPES = new HashMap<>();

    static {
        ALLOWED_IMAGE_TYPES.put("jpg", "image/jpg");
        ALLOWED_IMAGE_TYPES.put("jpeg", "image/jpeg");
        ALLOWED_IMAGE_TYPES.put("png", "image/png");
    }

    /**
     * 上传用户头像
     *
     * @param file   文件
     * @param userId 用户ID
     * @return 访问URL
     */
    public String uploadAvatar(MultipartFile file, Long userId) {
        // 1.验证文件
        validateFile(file);
        // 2.生成存储路径
        String objectName = generateAvatarObjectName(file, userId);
        // 3.确保存储桶存在
        ensureBucketExist(minioConfig.getBucketName());
        // 4.上传文件
        uploadFile(file, objectName);
        // 5.生成访问URL
        return getFileUrl(objectName);
    }

    private void validateFile(MultipartFile file) {
        // 非空判断
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_NULL);
        }

        // 大小判断
        if (file.getSize() > minioConfig.getFileSizeLimit()) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        // 类型判断，避免恶意上传exe
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException(ErrorCode.FILE_NAME_NULL);
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_IMAGE_TYPES.containsKey(extension)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED,
                    ErrorCode.FILE_TYPE_NOT_SUPPORTED.getMessage().replace("{0}", ALLOWED_IMAGE_TYPES.keySet().toString()));
        }
        try (InputStream inputStream = file.getInputStream()) {
            String fileType = FileTypeUtil.getType(inputStream);
            if (!ALLOWED_IMAGE_TYPES.containsValue("image/" + fileType)) {
                throw new BusinessException(ErrorCode.FILE_TYPE_MISMATCH);
            }
        } catch (IOException e) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
        }

    }

    /**
     * 生成存储对象名称
     *
     * @param file   用户上传的头像文件
     * @param userId 用户ID
     * @return 生成的存储对象名称，格式为：avatars/用户ID/日期路径/唯一ID.文件扩展名
     */
    private String generateAvatarObjectName(MultipartFile file, Long userId) {
        String extension = getFileExtension(file.getOriginalFilename());
        Long id = idGenerator.generateId(minioConfig.getBusinessKey());
        String datePath = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        return String.format("avatars/%d/%s/%s.%s", userId, datePath, id, extension);
    }


    /**
     * 确保存储桶存在
     */
    private void ensureBucketExist(String bucketName) {
        try {
            if (minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build())) {
                log.info("存储桶已存在: {}", bucketName);
                return;
            }
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());

            // 设置存储桶策略为公开读取
            String policy = MINIO_POLICY_AVATARS.formatted(bucketName);

            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config(policy)
                    .build());
        } catch (Exception e) {
            log.error("创建存储桶失败: {}", e.getMessage());
            throw new SystemException(ErrorCode.BUCKET_CREATE_FAILED, e);
        }
        log.info("创建存储桶: {}", bucketName);

    }

    private void uploadFile(MultipartFile file, String objectName) {
        try (InputStream inputStream = file.getInputStream()) {
            String contentType = ALLOWED_IMAGE_TYPES.get(
                    getFileExtension(file.getOriginalFilename()).toLowerCase());
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
            log.info("文件上传成功: {}", objectName);
        } catch (Exception e) {
            log.error("上传文件失败: {}", e.getMessage());
            throw new SystemException(ErrorCode.FILE_UPLOAD_FAILED, e);
        }

    }

    /**
     * 获取文件访问URL，配置Nginx代理，返回固定URL
     */
    private String getFileUrl(String objectName) {
        // 方式2：如果配置了nginx代理，返回固定URL（推荐生产环境）
        String url = String.format("http://petcare.com/%s/%s",
                minioConfig.getBucketName(), objectName);
        // 测试用：返回带签名的临时URL（有效期7天）
        // try {
        //     String tempUrl = minioClient.getPresignedObjectUrl(
        //             GetPresignedObjectUrlArgs.builder()
        //                     .method(Method.GET)
        //                     .bucket(minioConfig.getBucketName())
        //                     .object(objectName)
        //                     .expiry(7, TimeUnit.DAYS)
        //                     .build());
        // } catch (Exception e) {
        //     log.error("获取文件URL失败: {}", e.getMessage());
        //     throw new RuntimeException(e);
        // }
        return url;
    }

    /**
     * 删除头像文件
     */
    public void deleteAvatar(String objectUrl) throws Exception {
        // 从URL中提取objectName
        String objectName = extractObjectNameFromUrl(objectUrl);

        if (StrUtil.isNotBlank(objectName)) {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .build());
            log.info("删除文件: {}", objectName);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        return lastDot == -1 ? "" : filename.substring(lastDot + 1);
    }

    /**
     * 从URL提取objectName
     */
    private String extractObjectNameFromUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }

        // 解析URL，获取objectName部分
        String[] parts = url.split("/");
        if (parts.length < 2) {
            return null;
        }

        // 查找"avatars"之后的部分
        for (int i = 0; i < parts.length; i++) {
            if ("avatars".equals(parts[i])) {
                StringBuilder objectName = new StringBuilder();
                for (int j = i; j < parts.length; j++) {
                    objectName.append(parts[j]);
                    if (j < parts.length - 1) {
                        objectName.append("/");
                    }
                }
                return objectName.toString();
            }
        }

        return null;
    }
}
