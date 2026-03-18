# 大文件分片上传与断点续传实现方案

## 一、问题背景

AI 知识库文档上传存在以下问题：
1. 大文件（如 PDF、Word）上传超时
2. 网络中断后需要重新上传
3. 无法展示上传进度

## 二、技术方案

### 核心思路
- 文件分片：前端将大文件拆分成固定大小（如 5MB）的分片
- 并发上传：支持多分片并发上传，提升速度
- 断点续传：记录已上传分片，中断后可继续
- 服务端合并：所有分片上传完成后，由服务端合并

### 存储层统一抽象

由于项目同时使用阿里云 OSS 和 MinIO，两者都兼容 S3 协议，API 基本一致。

```
MultipartStorageTemplate (interface)
    ├── OssMultipartTemplateImpl
    └── MinioMultipartTemplateImpl
```

---

## 三、数据库设计

### 3.1 分片上传记录表

```sql
CREATE TABLE tb_file_upload_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    upload_id VARCHAR(100) NOT NULL COMMENT 'OSS/MinIO 返回的 uploadId',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    file_type VARCHAR(50) NOT NULL COMMENT '文件类型（md/pdf/docx等）',
    chunk_size INT NOT NULL COMMENT '分片大小（字节）',
    total_chunks INT NOT NULL COMMENT '总分片数',
    uploaded_chunks TEXT COMMENT '已上传分片索引（JSON数组）',
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADING' COMMENT '状态：UPLOADING/COMPLETED/FAILED/ABORTED',
    error_msg VARCHAR(500) COMMENT '错误信息',
    file_url VARCHAR(500) COMMENT '合并后的文件URL',
    created_by BIGINT COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_upload_id (upload_id),
    INDEX idx_created_by (created_by),
    INDEX idx_status (status)
) COMMENT '文件分片上传任务表';
```

---

## 四、架构设计

### 4.1 新增文件

**接口层**
- `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/storage/MultipartStorageTemplate.java`
- `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/storage/impl/OssMultipartTemplateImpl.java`
- `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/storage/impl/MinioMultipartTemplateImpl.java`

**实体层**
- `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/entity/FileUploadTask.java`

**DTO 层**
- `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/dto/FileUploadInitRequest.java`
- `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/dto/FileUploadInitResponse.java`
- `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/dto/FileUploadChunkRequest.java`

**Service 层**
- `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/service/FileUploadService.java`
- `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/service/impl/FileUploadServiceImpl.java`

**Controller 层**
- `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/controller/FileUploadController.java`

### 4.2 MultipartStorageTemplate 接口定义

```java
public interface MultipartStorageTemplate {

    /**
     * 初始化分片上传
     * @return uploadId
     */
    String initMultipartUpload(String bucket, String objectName);

    /**
     * 上传分片
     * @return partETag (分片编号 + ETag)
     */
    String uploadPart(String bucket, String objectName, String uploadId,
                     int partNumber, InputStream inputStream, long partSize);

    /**
     * 完成分片上传，合并文件
     * @return 文件访问URL
     */
    String completeMultipartUpload(String bucket, String objectName, String uploadId,
                                  List<String> partETags);

    /**
     * 取消分片上传
     */
    void abortMultipartUpload(String bucket, String objectName, String uploadId);

    /**
     * 列出已上传分片（断点续传用）
     */
    List<UploadedPart> listParts(String bucket, String objectName, String uploadId);
}
```

---

## 五、接口设计

### 5.1 初始化分片上传

```
POST /common/upload/init
```

**请求参数**：
```json
{
  "fileName": "knowledge-base.pdf",
  "fileSize": 52428800,
  "fileType": "pdf",
  "chunkSize": 5242880
}
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "taskId": 123456789,
    "uploadId": "oss-upload-id-xxx",
    "totalChunks": 10,
    "chunkSize": 5242880
  }
}
```

### 5.2 上传分片

```
POST /common/upload/chunk
```

**请求参数**：
```
taskId=123456789
chunkNumber=1
file=<binary>
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "taskId": 123456789,
    "chunkNumber": 1,
    "uploadedChunks": [1, 2, 3],
    "isCompleted": false
  }
}
```

### 5.3 完成上传

```
POST /common/upload/complete
```

**请求参数**：
```json
{
  "taskId": 123456789
}
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "taskId": 123456789,
    "fileUrl": "https://oss.example.com/ai/2026/03/13/xxx.pdf"
  }
}
```

### 5.4 取消上传

```
DELETE /common/upload/{taskId}
```

### 5.5 查询上传进度

```
GET /common/upload/{taskId}/progress
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "taskId": 123456789,
    "fileName": "knowledge-base.pdf",
    "totalChunks": 10,
    "uploadedChunks": [1, 2, 3, 5],
    "uploadedCount": 4,
    "status": "UPLOADING"
  }
}
```

---

## 六、断点续传实现

### 6.1 流程

1. **前端检测**：上传前调用 `/progress` 接口，检查是否存在未完成的任务
2. **获取进度**：根据 `uploadedChunks` 获取已上传分片
3. **继续上传**：跳过已上传分片，继续上传剩余分片
4. **完成合并**：所有分片上传完成后调用 `/complete`

### 6.2 过期清理

定时任务清理超过 24 小时未完成的任务：

```java
@Scheduled(cron = "0 0 * * * ?")
public void cleanExpiredTasks() {
    // 查询 status=UPLOADING 且 updated_at 超过 24 小时的任务
    // 调用 OSS/MinIO 的 abortMultipartUpload
    // 更新任务状态为 ABORTED
}
```

---

## 七、集成知识库上传

修改 `KnowledgeDocumentController.uploadDocument()`：

```java
@PostMapping("/upload")
public Result<KnowledgeDocumentResponse> uploadDocument(
    @RequestParam("file") MultipartFile file,
    @RequestParam(value = "taskId", required = false) Long taskId) {

    // 如果是小文件（< 10MB），直接上传
    if (file.getSize() < 10 * 1024 * 1024) {
        return Result.success(documentService.uploadDocument(file));
    }

    // 大文件走分片上传
    if (taskId == null) {
        // 初始化分片上传
        FileUploadInitResponse initResponse = fileUploadService.initUpload(file);
        return Result.success(initResponse);
    }

    // 完成上传并处理文档
    KnowledgeDocumentResponse response = documentService.completeMultipartUpload(taskId);
    return Result.success(response);
}
```

---

## 八、实施顺序

### 阶段一：数据库与实体
1. 创建 `tb_file_upload_task` 表
2. 创建 `FileUploadTask` 实体类
3. 创建 DTO 类

### 阶段二：存储层抽象
1. 创建 `MultipartStorageTemplate` 接口
2. 实现 `OssMultipartTemplateImpl`
3. 实现 `MinioMultipartTemplateImpl`

### 阶段三：业务层
1. 创建 `FileUploadService`
2. 实现分片上传核心逻辑
3. 实现断点续传逻辑
4. 添加定时清理任务

### 阶段四：接口层
1. 创建 `FileUploadController`
2. 实现 5 个接口

### 阶段五：知识库集成
1. 修改 `KnowledgeDocumentController`
2. 修改 `KnowledgeDocumentService`
3. 测试大文件上传

---

## 九、关键文件清单

### 新增文件
| 文件路径 |
|---------|
| `modules/pet-care-common/.../storage/MultipartStorageTemplate.java` |
| `modules/pet-care-common/.../storage/impl/OssMultipartTemplateImpl.java` |
| `modules/pet-care-common/.../storage/impl/MinioMultipartTemplateImpl.java` |
| `modules/pet-care-common/.../entity/FileUploadTask.java` |
| `modules/pet-care-common/.../dto/FileUploadInitRequest.java` |
| `modules/pet-care-common/.../dto/FileUploadInitResponse.java` |
| `modules/pet-care-common/.../dto/FileUploadChunkRequest.java` |
| `modules/pet-care-common/.../service/FileUploadService.java` |
| `modules/pet-care-common/.../service/impl/FileUploadServiceImpl.java` |
| `modules/pet-care-common/.../controller/FileUploadController.java` |

### 修改文件
| 文件路径 | 修改内容 |
|---------|---------|
| `modules/pet-care-ai/.../knowledge/controller/KnowledgeDocumentController.java` | 支持分片上传 |
| `modules/pet-care-ai/.../knowledge/service/impl/KnowledgeDocumentServiceImpl.java` | 支持分片处理 |

---

## 十、下次会话执行指令

**启动指令**：
```
请阅读 /Users/michael/IdeaProjects/petcare/docs/multipart-upload-plan.md
按照计划中的"阶段一：数据库与实体"开始实施
```

**关键上下文**：
- 项目路径：`/Users/michael/IdeaProjects/petcare`
- 分片大小：5MB
- 小文件阈值：10MB
- 支持存储：阿里云 OSS、MinIO
