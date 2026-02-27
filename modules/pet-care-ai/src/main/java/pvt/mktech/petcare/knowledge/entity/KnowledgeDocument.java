package pvt.mktech.petcare.knowledge.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import pvt.mktech.petcare.knowledge.entity.codelist.ProcessingStatusOfKnowledgeDocument;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * {@code @description}: 知识库文档实体（聚合根）
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Table("tb_knowledge_document")
@Data
public class KnowledgeDocument implements Serializable {
    @Serial
    private static final long serialVersionUID = 3177443417880263292L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Schema(description = "文档名称")
    private String name;

    @Schema(description = "文件Url")
    private String fileUrl;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "版本号")
    private Integer version = 1;

    @Schema(description = "状态：1-有效，0-禁用")
    private Integer status = 1;

    @Schema(description = "分块数量")
    private Integer chunkCount = 0;

    @Schema(description = "处理状态")
    private ProcessingStatusOfKnowledgeDocument processingStatus = ProcessingStatusOfKnowledgeDocument.PENDING;

    @Schema(description = "处理失败原因")
    private String processingError;

    @Schema(description = "创建时间")
    @Column(value = "created_at", onInsertValue = "CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @Column(value = "updated_at", onInsertValue = "CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除：0-正常，1-已删除
     */
    @Column(value = "is_deleted", onInsertValue = "0")
    private Boolean isDeleted;

    /**
     * 删除时间
     */
    @Column(value = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 上传文档（业务方法）
     */
    public void upload(String fileName, String fileUrl, String fileType, Long fileSize) {
        this.name = fileName;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.status = 1;
        this.version = 1;
        this.isDeleted = false;
        this.processingStatus = ProcessingStatusOfKnowledgeDocument.PROCESSING;
        this.chunkCount = 0;
        this.processingError = null;
    }

    /**
     * 删除文档（业务方法）
     */
    public void delete() {
        this.status = 0;
        this.isDeleted = false;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 判断文档是否有效
     */
    public boolean isActive() {
        return status != null && status == 1 && (isDeleted == null || !isDeleted);
    }

    /**
     * 禁用文档
     */
    public void disable() {
        this.status = 0;
    }

    /**
     * 启用文档
     */
    public void enable() {
        this.status = 1;
    }

    /**
     * 更新向量处理成功
     */
    public void updateProcessSuccess(Integer chunkCount) {
        this.processingStatus = ProcessingStatusOfKnowledgeDocument.COMPLETED;
        this.chunkCount = chunkCount;
        this.processingError = null;
    }

    /**
     * 更新向量处理失败
     */
    public void updateProcessFailure(String error) {
        this.processingStatus = ProcessingStatusOfKnowledgeDocument.FAILED;
        this.processingError = error;
    }

    /**
     * 判断是否处理完成
     */
    public boolean isProcessed() {
        return ProcessingStatusOfKnowledgeDocument.COMPLETED.equals(processingStatus);
    }

    /**
     * 判断是否处理中
     */
    public boolean isProcessing() {
        return ProcessingStatusOfKnowledgeDocument.PROCESSING.equals(processingStatus);
    }
}
