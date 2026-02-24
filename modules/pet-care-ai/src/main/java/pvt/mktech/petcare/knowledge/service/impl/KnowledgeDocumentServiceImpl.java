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
import org.springframework.core.io.InputStreamResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pvt.mktech.petcare.knowledge.dto.response.KnowledgeDocumentResponse;
import pvt.mktech.petcare.knowledge.entity.KnowledgeDocument;
import pvt.mktech.petcare.knowledge.mapper.KnowledgeDocumentMapper;
import pvt.mktech.petcare.knowledge.service.KnowledgeDocumentService;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.storage.OssTemplate;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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
    private final VectorStore elasticsearchVectorStore;
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
        save(document);

        try {
            ossTemplate.deleteFile(document.getFileUrl());
        } catch (Exception e) {
            log.warn("删除OSS文件失败: {}", document.getFileUrl(), e);
        }

        // TODO: 删除 ES 中的向量数据
        log.info("文档已删除: id={}, name={}", id, document.getName());
    }

    /**
     * 异步处理文档到向量存储
     * 降低 batchSize 避免智谱 API 超时
     */
    @Async("vectorProcessExecutor")
    public void processDocumentToVectorStoreAsync(Long documentId, MultipartFile file) {
        List<String> processedDocIds = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream()) {
            log.info("开始异步处理文档向量: documentId={}", documentId);

            // 1. 提取文件名和 tag
            String fileName = StrUtil.isBlank(file.getOriginalFilename()) ?
                    "unknown_file" : file.getOriginalFilename();
            String tag = extractTag(fileName);
            log.info("文档信息: fileName={}, tag={}", fileName, tag);

            // 2. 读取 markdown 文件
            MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                    .withHorizontalRuleCreateDocument(true)
                    .withIncludeCodeBlock(false)
                    .withIncludeBlockquote(false)
                    .withAdditionalMetadata("filename", fileName)
                    .withAdditionalMetadata("tag", tag)
                    .withAdditionalMetadata("documentId", documentId.toString())
                    .build();
            MarkdownDocumentReader reader = new MarkdownDocumentReader(
                    new InputStreamResource(inputStream), config);
            List<Document> documents = reader.get();

            // 3. 文档拆分
            TokenTextSplitter textSplitter = new TokenTextSplitter();
            List<Document> splitDocuments = textSplitter.apply(documents);
            log.info("文档拆分后数量: {}", splitDocuments.size());

            // 4. 批量处理（降低 batchSize 避免超时）
            int batchSize = 3;
            for (int i = 0; i < splitDocuments.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, splitDocuments.size());
                List<Document> batch = splitDocuments.subList(i, endIndex);

                // 补充关键词元信息
                List<Document> enrichedBatch = keywordMetadataEnricher.apply(batch);

                // 添加到向量存储
                elasticsearchVectorStore.add(enrichedBatch);

                // 记录文档 ID 用于回滚
                enrichedBatch.forEach(doc -> processedDocIds.add(doc.getId()));
            }

            // 5. 更新处理成功状态
            KnowledgeDocument document = getById(documentId);
            if (document != null) {
                document.updateProcessSuccess(splitDocuments.size());
                updateById(document);
            }
            log.info("文档向量处理完成: documentId={}, chunkCount={}", documentId, splitDocuments.size());

        } catch (Exception e) {
            log.error("文档向量处理失败: documentId={}", documentId, e);

            // 回滚已写入的向量数据
            rollbackVectorStore(processedDocIds);

            // 更新处理失败状态
            String error = e.getMessage();
            KnowledgeDocument document = getById(documentId);
            if (document != null) {
                // 截取错误信息，避免超过数据库字段长度限制
                String shortError = error != null && error.length() > 450
                        ? error.substring(0, 450) + "..."
                        : error;
                document.updateProcessFailure(shortError);
                updateById(document);
            }
        }
    }

    /**
     * 回滚向量存储中的数据
     */
    private void rollbackVectorStore(List<String> docIds) {
        if (docIds == null || docIds.isEmpty()) {
            return;
        }
        try {
            elasticsearchVectorStore.delete(docIds);
            log.info("向量回滚完成: count={}", docIds.size());
        } catch (Exception e) {
            log.warn("向量回滚失败: count={}", docIds.size(), e);
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
}
