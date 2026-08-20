package pvt.mktech.petcare.knowledge.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.InputStreamResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import pvt.mktech.petcare.knowledge.dto.response.KnowledgeDocumentResponse;
import pvt.mktech.petcare.knowledge.entity.KnowledgeDocument;
import pvt.mktech.petcare.knowledge.entity.codelist.ProcessingStatusOfKnowledgeDocument;
import pvt.mktech.petcare.knowledge.mapper.KnowledgeDocumentMapper;
import pvt.mktech.petcare.knowledge.service.KnowledgeDocumentService;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.storage.OssTemplate;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static pvt.mktech.petcare.knowledge.entity.table.KnowledgeDocumentTableDef.DOCUMENT;

/**
 * {@code @description}: 知识库文档服务实现
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentServiceImpl extends ServiceImpl<KnowledgeDocumentMapper, KnowledgeDocument> implements KnowledgeDocumentService {

    private final OssTemplate ossTemplate;
    private final VectorStore vectorStore;
    private final KeywordMetadataEnricher keywordMetadataEnricher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentResponse uploadDocument(MultipartFile file) {
        // 暂时仅支持 MD格式文件
        String fileName = file.getOriginalFilename();
        String fileType = getFileExtension(fileName);
        if (!"md".equals(fileType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件格式错误，请上传Markdown格式文件");
        }

        long startTime = System.currentTimeMillis();
        String fileUrl = ossTemplate.uploadDocument(file);
        log.info("文档上传OSS耗时: {}ms", System.currentTimeMillis() - startTime);

        KnowledgeDocument document = new KnowledgeDocument();
        document.upload(fileName, fileUrl, fileType, file.getSize());
        save(document);

        // 异步处理向量
        processDocumentToVectorStoreAsync(document.getId(), file);

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

        document.delete();
        if (!updateById(document)) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "更新文档删除状态失败");
        }
        deleteVectorsByDocumentId(id);
        deleteOssAfterCommit(document.getFileUrl());
        log.info("文档已删除: id={}, name={}", id, document.getName());
    }

    /**
     * 异步处理文档到向量存储
     * 降低 batchSize 避免智谱 API 超时
     */
    @Async("vectorProcessExecutor")
    public void processDocumentToVectorStoreAsync(Long documentId, MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            processDocumentToVectorStore(documentId, inputStream, file.getOriginalFilename());

        } catch (Exception e) {
            markProcessingFailed(documentId, e);
        }
    }

    private void processDocumentToVectorStore(Long documentId, InputStream inputStream, String originalFilename) throws Exception {
        String fileName = StrUtil.isBlank(originalFilename) ? "unknown_file" : originalFilename;
        try {
            deleteVectorsByDocumentId(documentId);
            MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                    .withHorizontalRuleCreateDocument(true).withIncludeCodeBlock(false).withIncludeBlockquote(false)
                    .withAdditionalMetadata("filename", fileName).withAdditionalMetadata("tag", extractTag(fileName))
                    .withAdditionalMetadata("document_id", documentId.toString()).build();
            List<Document> splitDocuments = new TokenTextSplitter().apply(
                    new MarkdownDocumentReader(new InputStreamResource(inputStream), config).get());
            for (int i = 0; i < splitDocuments.size(); i += 3) {
                List<Document> enrichedBatch = keywordMetadataEnricher.apply(splitDocuments.subList(i, Math.min(i + 3, splitDocuments.size())));
                for (int chunkIndex = 0; chunkIndex < enrichedBatch.size(); chunkIndex++) {
                    Document enriched = enrichedBatch.get(chunkIndex);
                    Map<String, Object> metadata = new HashMap<>(enriched.getMetadata());
                    metadata.put("document_id", documentId.toString());
                    metadata.put("chunk_index", i + chunkIndex);
                    String chunkId = UUID.nameUUIDFromBytes((documentId + ":" + (i + chunkIndex)).getBytes(StandardCharsets.UTF_8)).toString();
                    enrichedBatch.set(chunkIndex, new Document(chunkId, enriched.getText(), metadata));
                }
                vectorStore.add(enrichedBatch);
            }
            KnowledgeDocument document = getById(documentId);
            if (document != null) {
                document.updateProcessSuccess(splitDocuments.size());
                updateById(document);
            }
            log.info("文档向量处理完成: documentId={}, chunkCount={}", documentId, splitDocuments.size());
        } catch (Exception e) {
            try {
                deleteVectorsByDocumentId(documentId);
            } catch (Exception cleanupException) {
                e.addSuppressed(cleanupException == e
                        ? new IllegalStateException("向量清理失败", cleanupException)
                        : cleanupException);
                log.error("向量整体清理失败: documentId={}", documentId, cleanupException);
            }
            throw e;
        }
    }

    private void markProcessingFailed(Long documentId, Exception exception) {
        log.error("文档向量处理失败: documentId={}", documentId, exception);
        KnowledgeDocument document = getById(documentId);
        if (document != null) {
            String error = exception.getMessage();
            if (exception.getSuppressed().length > 0) {
                error = error + "; 向量清理失败: " + exception.getSuppressed()[0].getMessage();
            }
            document.updateProcessFailure(shortError(error));
            updateById(document);
        }
    }

    private Filter.Expression documentIdFilter(Long documentId) {
        return new FilterExpressionBuilder().eq("document_id", documentId.toString()).build();
    }

    private void deleteVectorsByDocumentId(Long documentId) {
        vectorStore.delete(documentIdFilter(documentId));
    }

    private String shortError(String error) {
        return error != null && error.length() > 450 ? error.substring(0, 450) + "..." : error;
    }

    private void deleteOssAfterCommit(String fileUrl) {
        Runnable deleteOss = () -> {
            try {
                ossTemplate.deleteFile(fileUrl);
            } catch (Exception e) {
                log.error("删除OSS文件失败: {}", fileUrl, e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { deleteOss.run(); }
            });
        } else {
            deleteOss.run();
        }
    }

    /**
     * 提取文件名中的 tag
     * 格式: xxx-tag.md，提取 tag 部分
     */
    private String extractTag(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return "default";
        }
        String tag = StrUtil.subBefore(fileName, ".md", false);
        tag = StrUtil.subAfter(tag, "-", false);
        return StrUtil.isBlank(tag) ? "default" : tag;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        return lastDot == -1 ? "" : filename.substring(lastDot + 1).toLowerCase();
    }

    /**
     * 转换为响应 DTO
     */
    private KnowledgeDocumentResponse convertToResponse(KnowledgeDocument document) {
        KnowledgeDocumentResponse response = new KnowledgeDocumentResponse();
        BeanUtil.copyProperties(document, response);
        return response;
    }

    @Override
    public void reindexDocument(Long id) {
        KnowledgeDocument document = getOne(DOCUMENT.ID.eq(id));
        if (document == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "文档不存在");
        }

        try (InputStream inputStream = ossTemplate.getInputStreamByUrl(document.getFileUrl())) {
            processDocumentToVectorStore(id, inputStream, document.getName());
        } catch (Exception e) {
            markProcessingFailed(id, e);
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "文档重建向量失败: " + e.getMessage());
        }
    }
}
