package pvt.mktech.petcare.ai.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

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
    private Integer isDeleted;

    /**
     * 删除时间
     */
    @Column(value = "deleted_at")
    private LocalDateTime deletedAt;
}

