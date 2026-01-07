package pvt.mktech.petcare.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeDocumentResponse {
    private Long id;
    private String name;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private Integer version;
    private Integer status;
    private Integer chunkCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

