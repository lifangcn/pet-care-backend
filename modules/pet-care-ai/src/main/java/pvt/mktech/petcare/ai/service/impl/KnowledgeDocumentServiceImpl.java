package pvt.mktech.petcare.ai.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
//import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pvt.mktech.petcare.ai.dto.KnowledgeDocumentResponse;
import pvt.mktech.petcare.ai.entity.KnowledgeDocument;
import pvt.mktech.petcare.ai.mapper.KnowledgeDocumentMapper;
import pvt.mktech.petcare.ai.service.KnowledgeDocumentService;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.exception.SystemException;
import pvt.mktech.petcare.common.storage.OssTemplate;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import static pvt.mktech.petcare.ai.entity.table.KnowledgeDocumentTableDef.DOCUMENT;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentServiceImpl extends ServiceImpl<KnowledgeDocumentMapper, KnowledgeDocument> implements KnowledgeDocumentService {

    private final OssTemplate ossTemplate;
    // Milvus 暂时禁用
    // private final MilvusVectorStore milvusVectorStore;

    @Override
    @Transactional()
    public KnowledgeDocumentResponse uploadDocument(MultipartFile file) {
        String fileUrl = ossTemplate.uploadDocument(file);
        String fileName = file.getOriginalFilename();
        String fileType = getFileExtension(fileName);

        KnowledgeDocument document = new KnowledgeDocument();
        document.setName(fileName);
        document.setFileUrl(fileUrl);
        document.setFileType(fileType);
        document.setFileSize(file.getSize());
        document.setVersion(1);
        document.setStatus(1);

        save(document);

//        processDocumentToVectorStore(document.getId());

        return convertToResponse(document);
    }

    @Override
    public List<KnowledgeDocumentResponse> listDocuments() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(DOCUMENT.STATUS.eq(1))
                .orderBy(DOCUMENT.CREATED_AT.desc());
        List<KnowledgeDocument> documents = list(queryWrapper);
        return documents.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public KnowledgeDocumentResponse getDocument(Long id) {
        KnowledgeDocument document = getOne(DOCUMENT.ID.eq(id).and(DOCUMENT.STATUS.eq(1)));
        if (document == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "文档不存在");
        }
        return convertToResponse(document);
    }

    @Override
    @Transactional
    public void deleteDocument(Long id) {
        KnowledgeDocument document = getOne(DOCUMENT.ID.eq(id));
        if (document == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "文档不存在");
        }

        document.setStatus(0);
        save(document);

        try {
            ossTemplate.deleteFile(document.getFileUrl());
        } catch (Exception e) {
            log.warn("删除MinIO文件失败: {}", document.getFileUrl(), e);
        }

        deleteDocumentFromVectorStore(document.getId());

        log.info("文档已删除: id={}, name={}", id, document.getName());
    }

    @Override
    public void processDocumentToVectorStore(Long documentId) {
        KnowledgeDocument document = getOne(DOCUMENT.ID.eq(documentId));
        if (document == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "文档不存在");
        }

        try {
            InputStream inputStream = ossTemplate.getInputStreamByUrl(document.getFileUrl());

            TikaDocumentReader reader = new TikaDocumentReader(new InputStreamResource(inputStream));
            List<Document> documents = reader.get();

            TextSplitter textSplitter = new TokenTextSplitter();
            List<Document> chunks = textSplitter.apply(documents);

            List<Document> documentsWithMetadata = chunks.stream()
                    .filter(chunk -> chunk.getText() != null && !chunk.getText().isEmpty())
                    .map(chunk -> {
                        Document doc = new Document(
                                chunk.getText(),
                                chunk.getMetadata()
                        );
                        doc.getMetadata().put("documentId", documentId.toString());
                        doc.getMetadata().put("documentName", document.getName());
                        return doc;
                    })
                    .collect(Collectors.toList());

            // Milvus 暂时禁用
            log.warn("Milvus 未启用，跳过向量存储");
            /*if (milvusVectorStore != null) {
                milvusVectorStore.add(documentsWithMetadata);
            } else {
                log.warn("Milvus 未启用，跳过向量存储");
            }*/

            document.setChunkCount(chunks.size());
            updateById(document);

            log.info("文档已处理: id={}, name={}, chunks={}",
                    documentId, document.getName(), chunks.size());
        } catch (Exception e) {
            log.error("处理文档到向量数据库失败: id={}", documentId, e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "文档处理失败", e);
        }
    }

    private void deleteDocumentFromVectorStore(Long documentId) {
        // Milvus 暂时禁用
        log.warn("Milvus 未启用，跳过向量删除");
        /*try {
            List<Document> documents = milvusVectorStore.similaritySearch("documentId:" + documentId);
            if (!documents.isEmpty()) {
                List<String> ids = documents.stream()
                        .map(Document::getId)
                        .collect(Collectors.toList());
                if (!ids.isEmpty()) {
                    milvusVectorStore.delete(ids);
                }
            }
            log.info("从向量数据库删除文档: id={}", documentId);
        } catch (Exception e) {
            log.warn("从向量数据库删除文档失败: id={}", documentId, e);
        }*/
    }

    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        return lastDot == -1 ? "" : filename.substring(lastDot + 1).toLowerCase();
    }

    private KnowledgeDocumentResponse convertToResponse(KnowledgeDocument document) {
        KnowledgeDocumentResponse response = new KnowledgeDocumentResponse();
        BeanUtil.copyProperties(document, response);
        return response;
    }
}

