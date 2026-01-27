package pvt.mktech.petcare.knowledgedocument.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pvt.mktech.petcare.knowledgedocument.dto.KnowledgeDocumentResponse;
import pvt.mktech.petcare.knowledgedocument.entity.KnowledgeDocument;
import pvt.mktech.petcare.knowledgedocument.mapper.KnowledgeDocumentMapper;
import pvt.mktech.petcare.knowledgedocument.service.KnowledgeDocumentService;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.exception.SystemException;
import pvt.mktech.petcare.common.storage.OssTemplate;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import static pvt.mktech.petcare.knowledgedocument.entity.table.KnowledgeDocumentTableDef.DOCUMENT;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentServiceImpl extends ServiceImpl<KnowledgeDocumentMapper, KnowledgeDocument> implements KnowledgeDocumentService {

    private final OssTemplate ossTemplate;
    private final VectorStore elasticsearchVectorStore;
    private final ChatModel dashscopeChatModel;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentResponse uploadDocument(MultipartFile file) {

        // 暂时仅支持 MD格式文件
        String fileName = file.getOriginalFilename();
        String fileType = getFileExtension(fileName);
        if (!fileType.equals("md")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件格式错误，请上传Markdown格式文件");
        }

        // TODO 加入消息队列进行解耦，提高响应速度
        long startTime = System.currentTimeMillis();
        String fileUrl = ossTemplate.uploadDocument(file);
        log.info("文档上传耗时: {}ms", System.currentTimeMillis() - startTime);

        KnowledgeDocument document = new KnowledgeDocument();
        document.setName(fileName);
        document.setFileUrl(fileUrl);
        document.setFileType(fileType);
        document.setFileSize(file.getSize());
        document.setVersion(1);
        document.setStatus(1);
        
        startTime = System.currentTimeMillis();
        Integer chunkCount = processDocumentToVectorStore(file);
        log.info("文档处理到向量库耗时: {}ms", System.currentTimeMillis() - startTime);

        
//        startTime = System.currentTimeMillis();
//        document.setChunkCount(chunkCount);
//        save(document);
//        log.info("文档信息保存耗时: {}ms", System.currentTimeMillis() - startTime);
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

//        deleteDocumentFromVectorStore(document.getId());

        log.info("文档已删除: id={}, name={}", id, document.getName());
    }

    /**
     *
     * @param file
     * @return
     */
    public Integer processDocumentToVectorStore(MultipartFile file) {

        try (InputStream inputStream = file.getInputStream()) {
            // 1.提取文件名 - .md 之间的尾缀
            String fileName = StrUtil.isBlank(file.getOriginalFilename()) ?
                    "unknown_file" : file.getOriginalFilename();
            String tag = StrUtil.subBetween(fileName, "-", ".md");
            log.info("fileName: {} tag: {}", fileName, tag);

            // 2.读取 markdown 文件
            MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                    .withHorizontalRuleCreateDocument(true)
                    .withIncludeCodeBlock(false)
                    .withIncludeBlockquote(false)
                    .withAdditionalMetadata("filename", fileName)
                    .withAdditionalMetadata("tag", tag)
                    .build();
            MarkdownDocumentReader reader = new MarkdownDocumentReader(
                    new InputStreamResource(inputStream), config);
            List<Document> documents = reader.get();

            // 3.文档拆分
            TokenTextSplitter textSplitter = new TokenTextSplitter();
            List<Document> splitDocuments = textSplitter.apply(documents);
            // 百炼API单批次添加上限为10
            int batchSize = 10;
            for (int i = 0; i < splitDocuments.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, splitDocuments.size());
                List<Document> batch = splitDocuments.subList(i, endIndex);

                // 5.补充关键词元信息
                KeywordMetadataEnricher keywordMetadataEnricher = new KeywordMetadataEnricher(dashscopeChatModel, 5);
                List<Document> enrichedBatch = keywordMetadataEnricher.apply(batch);

                // 6.添加到向量存储
                elasticsearchVectorStore.add(enrichedBatch);
            }

            return splitDocuments.size();
        } catch (Exception e) {
            log.error("处理文档到向量数据库失败", e);
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

