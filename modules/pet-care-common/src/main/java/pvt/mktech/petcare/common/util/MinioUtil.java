package pvt.mktech.petcare.common.util;

import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.util.StrUtil;
import io.minio.*;
import io.minio.http.Method;
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
import java.util.concurrent.TimeUnit;

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

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final String MINIO_POLICY_AVATARS = """
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
    private static final String MINIO_POLICY_AI_FILE = """
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
                    "arn:aws:s3:::%s/ai/*"
                  ]
                }
              ]
            }
            """;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final DistributedIdGenerator idGenerator;

    private static final Map<String, String> ALLOWED_IMAGE_TYPES = new HashMap<>() {{
        put("jpg", "image/jpeg");
        put("jpeg", "image/jpeg");
        put("png", "image/png");
    }};

    private static final Map<String, String> ALLOWED_DOCUMENT_TYPES = new HashMap<>() {{
        put("md", "text/markdown");
        put("pdf", "application/pdf");
        put("doc", "application/msword");
        put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        put("ppt", "application/vnd.ms-powerpoint");
        put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        put("xls", "application/vnd.ms-excel");
        put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        put("txt", "text/plain");
        put("rtf", "application/rtf");
    }};

    /**
     * 上传图片（用于头像上传）
     *
     * @param file 文件
     * @param id   主键ID
     * @return 访问URL
     */
    public String uploadAvatar(MultipartFile file, Long id) {
        validateFile(file, ALLOWED_IMAGE_TYPES, true);
        String objectName = generateObjectName("avatars", file, id);
        ensureBucketExist(MINIO_POLICY_AVATARS, minioConfig.getBucketName());
        return uploadFile(file, objectName, ALLOWED_IMAGE_TYPES, "头像");
    }

    /**
     * 上传文档（RAG知识库上传文档）
     *
     * @param file 文件
     * @return 访问URL
     */
    public String uploadDocument(MultipartFile file) {
        validateFile(file, ALLOWED_DOCUMENT_TYPES, false);
        String objectName = generateObjectName("ai", file, null);
        ensureBucketExist(MINIO_POLICY_AI_FILE, minioConfig.getBucketName());
        return uploadFile(file, objectName, ALLOWED_DOCUMENT_TYPES, "文档");
    }


    public InputStream getInputStreamByUrl(String fileUrl) throws Exception {
        if (StrUtil.isBlank(fileUrl)) {
            throw new BusinessException(ErrorCode.FILE_URL_NULL);
        }

        String objectName = null;
        String[] parts = fileUrl.split("/");
        String bucketName = minioConfig.getBucketName();
        int bucketIndex = -1;
        for (int i = 0; i < parts.length; i++) {
            if (bucketName.equals(parts[i])) {
                bucketIndex = i;
                break;
            }
        }
        if (bucketIndex != -1 && bucketIndex < parts.length - 1) {
            StringBuilder objectName1 = new StringBuilder();
            for (int j = bucketIndex + 1; j < parts.length; j++) {
                objectName1.append(parts[j]);
                if (j < parts.length - 1) {
                    objectName1.append("/");
                }
            }
            objectName = objectName1.toString();
        }
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(objectName)
                .build());
    }

    private String uploadFile(MultipartFile file, String objectName, Map<String, String> typeMap, String fileType) {
        try (InputStream inputStream = file.getInputStream()) {
            String extension = getFileExtension(file.getOriginalFilename()).toLowerCase();
            String contentType = typeMap.get(extension);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
            log.info("{}上传成功: {}", fileType, objectName);
            return getFileUrl(objectName);
        } catch (Exception e) {
            log.error("{}上传失败: {}", fileType, e.getMessage());
            throw new SystemException(ErrorCode.FILE_UPLOAD_FAILED, e);
        }
    }

    private void validateFile(MultipartFile file, Map<String, String> allowedTypeMap, boolean isImage) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_NULL);
        }

        if (file.getSize() > minioConfig.getFileSizeLimit()) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException(ErrorCode.FILE_NAME_NULL);
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!allowedTypeMap.containsKey(extension)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED,
                    ErrorCode.FILE_TYPE_NOT_SUPPORTED.getMessage().replace("{0}", allowedTypeMap.keySet().toString()));
        }

        if (isImage) {
            validateImageFileType(file, extension);
        }
    }

    private void validateImageFileType(MultipartFile file, String extension) {
        try (InputStream inputStream = file.getInputStream()) {
            String detectedType = FileTypeUtil.getType(inputStream);
            if (detectedType == null) {
                throw new BusinessException(ErrorCode.FILE_TYPE_MISMATCH);
            }
            detectedType = detectedType.toLowerCase();
            String expectedMimeType = ALLOWED_IMAGE_TYPES.get(extension);
            if (expectedMimeType == null) {
                throw new BusinessException(ErrorCode.FILE_TYPE_MISMATCH);
            }
            String expectedType = expectedMimeType.substring("image/".length()).toLowerCase();
            boolean isValid = expectedType.equals(detectedType) ||
                    (extension.equals("jpg") && detectedType.equals("jpeg")) ||
                    (extension.equals("jpeg") && detectedType.equals("jpg"));
            if (!isValid) {
                throw new BusinessException(ErrorCode.FILE_TYPE_MISMATCH);
            }
        } catch (IOException e) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
        }
    }

    private String generateObjectName(String businessKey, MultipartFile file, Long id) {
        String extension = getFileExtension(file.getOriginalFilename());
        Long fileId = idGenerator.generateId(minioConfig.getBusinessKey());
        String datePath = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        if (id != null) {
            return String.format("%s/%d/%s/%s.%s", businessKey, id, datePath, fileId, extension);
        } else {
            return String.format("%s/%s/%s.%s", businessKey, datePath, fileId, extension);
        }
    }


    /**
     * 确保存储桶存在
     */
    private void ensureBucketExist(String minioPolicy, String bucketName) {
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
            String policy = minioPolicy.formatted(bucketName);

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

    /**
     * 获取文件访问URL，配置Nginx代理，返回固定URL
     */
    private String getFileUrl(String objectName) {
        return String.format("http://petcare.com/%s/%s", minioConfig.getBucketName(), objectName);
    }

    /**
     * 删除文件
     */
    public void deleteFile(String objectUrl) throws Exception {
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
     * 从URL中提取objectName
     * @param url 文件URL
     * @return objectName
     */
    private String extractObjectNameFromUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }

        String[] parts = url.split("/");
        if (parts.length < 2) {
            return null;
        }

        String bucketName = minioConfig.getBucketName();
        int bucketIndex = -1;
        for (int i = 0; i < parts.length; i++) {
            if (bucketName.equals(parts[i])) {
                bucketIndex = i;
                break;
            }
        }

        if (bucketIndex == -1 || bucketIndex >= parts.length - 1) {
            return null;
        }

        StringBuilder objectName = new StringBuilder();
        for (int j = bucketIndex + 1; j < parts.length; j++) {
            objectName.append(parts[j]);
            if (j < parts.length - 1) {
                objectName.append("/");
            }
        }
        return objectName.toString();
    }

    public String generatePreviewUrl(String fileUrl) {
        // 返回带签名的临时URL（有效期1天）
        try {
            String tempUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioConfig.getBucketName())
                            .object(extractObjectNameFromUrl(fileUrl))
                            .expiry(1, TimeUnit.HOURS)
                            .build());
            return tempUrl;
        } catch (Exception e) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "生成预签名URL失败");
        }
    }
}
