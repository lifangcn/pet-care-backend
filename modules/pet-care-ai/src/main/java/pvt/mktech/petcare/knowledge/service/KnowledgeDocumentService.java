package pvt.mktech.petcare.knowledge.service;

import com.mybatisflex.core.service.IService;
import org.springframework.web.multipart.MultipartFile;
import pvt.mktech.petcare.knowledge.dto.response.KnowledgeDocumentResponse;
import pvt.mktech.petcare.knowledge.entity.KnowledgeDocument;

import java.util.List;

/**
 * {@code @description}: 知识库文档服务接口
 * {@code @date}: 2026-01-30
 * @author Michael
 */
public interface KnowledgeDocumentService extends IService<KnowledgeDocument> {

    /**
     * 上传文档
     */
    KnowledgeDocumentResponse uploadDocument(MultipartFile file);

    /**
     * 查询文档列表
     */
    List<KnowledgeDocumentResponse> listDocuments();

    /**
     * 查询文档详情
     */
    KnowledgeDocumentResponse getDocument(Long id);

    /**
     * 删除文档
     */
    void deleteDocument(Long id);
}
