package pvt.mktech.petcare.common.storage;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.exception.SystemException;
import pvt.mktech.petcare.common.snowflake.SnowflakeIdGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code @description}: OSS 工具类
 * {@code @date}: 2026/1/8 19:06
 *
 * @author Michael
 */
@Slf4j
public class OssTemplate {

    private final OssProperties ossProperties;
    private final OSS ossClient;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public OssTemplate(OSS ossClient,
                       OssProperties ossProperties,
                       SnowflakeIdGenerator snowflakeIdGenerator) {
        this.ossClient = ossClient;
        this.ossProperties = ossProperties;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
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
        return uploadFile(file, objectName, ALLOWED_DOCUMENT_TYPES, "文档");
    }

    private void validateFile(MultipartFile file, Map<String, String> allowedTypeMap, boolean isImage) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_NULL);
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

    /**
     * 上传文件逻辑
     *
     * @param file       文件
     * @param objectName 对象名
     * @param typeMap    文件类型集合
     * @param fileType   文件类型
     * @return 访问URL
     */
    private String uploadFile(MultipartFile file, String objectName, Map<String, String> typeMap, String fileType) {
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectResult putObjectResult = ossClient.putObject(ossProperties.getBucketName(), objectName, inputStream);
            String url = generateAccessUrl(objectName);
            log.info("{}上传成功: {}", fileType, url);
            return url;
        } catch (Exception e) {
            log.error("{}上传失败: {}", fileType, e.getMessage());
            throw new SystemException(ErrorCode.FILE_UPLOAD_FAILED, e);
        }
    }


    public InputStream getInputStreamByUrl(String fileUrl) throws Exception {
        if (StrUtil.isBlank(fileUrl)) {
            throw new BusinessException(ErrorCode.FILE_URL_NULL);
        }
        String objectName = extractObjectNameFromUrl(fileUrl);
        OSSObject object = ossClient.getObject(ossProperties.getBucketName(), objectName);
        return object.getObjectContent();
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
        long fileId = snowflakeIdGenerator.nextId();
        String datePath = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        if (id != null) {
            return String.format("%s/%d/%s/%s.%s", businessKey, id, datePath, fileId, extension);
        } else {
            return String.format("%s/%s/%s.%s", businessKey, datePath, fileId, extension);
        }
    }

    /**
     * 生成文件访问 URL (如果是私有 Bucket，需要生成签名 URL)
     */
    public String generateAccessUrl(String objectName) {
        // 设置 URL 过期时间，例如 1 小时
        return ossClient.generatePresignedUrl(
                ossProperties.getBucketName(),
                objectName, new DateTime().offset(DateField.MONTH, 1)
        ).toString();
    }

    /**
     * 删除文件
     */
    public void deleteFile(String objectUrl) throws Exception {
        // 从URL中提取objectName
        String objectName = extractObjectNameFromUrl(objectUrl);

        if (StrUtil.isNotBlank(objectName)) {
            ossClient.deleteObject(ossProperties.getBucketName(), objectName);
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
     *
     * @param url 文件URL
     * @return objectName
     */
    private String extractObjectNameFromUrl(String url) {
        String path = URLUtil.getPath(url);
        return StrUtil.sub(path, 1, path.length());
    }
}
