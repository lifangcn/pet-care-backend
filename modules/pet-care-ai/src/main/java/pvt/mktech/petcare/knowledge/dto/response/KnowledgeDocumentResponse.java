package pvt.mktech.petcare.knowledge.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * {@code @description}: 知识库文档响应 DTO
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Data
public class KnowledgeDocumentResponse {
    private Long id;
    private String name;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private Integer version;
    private String status;
    private Integer chunkCount;
    private String processingStatus;
    private String processingError;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
