package pvt.mktech.petcare.ai.service;

import com.mybatisflex.core.service.IService;
import org.springframework.web.multipart.MultipartFile;
import pvt.mktech.petcare.ai.dto.KnowledgeDocumentResponse;
import pvt.mktech.petcare.ai.entity.KnowledgeDocument;

import java.util.List;

public interface KnowledgeDocumentService extends IService<KnowledgeDocument> {

    KnowledgeDocumentResponse uploadDocument(MultipartFile file);

    List<KnowledgeDocumentResponse> listDocuments();

    KnowledgeDocumentResponse getDocument(Long id);

    void deleteDocument(Long id);
}

