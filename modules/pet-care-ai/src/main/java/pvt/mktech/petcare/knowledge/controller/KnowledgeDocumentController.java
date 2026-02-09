package pvt.mktech.petcare.knowledge.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.common.storage.OssTemplate;
import pvt.mktech.petcare.knowledge.dto.response.KnowledgeDocumentResponse;
import pvt.mktech.petcare.knowledge.service.KnowledgeDocumentService;

import java.util.List;

import static pvt.mktech.petcare.common.dto.response.ResultCode.NOT_FOUND;

/**
 * {@code @description}: 知识库文档控制器
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Slf4j
@RestController
@RequestMapping("/ai/document")
@RequiredArgsConstructor
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService documentService;
    private final OssTemplate ossTemplate;

    @PostMapping("/upload")
    public Result<KnowledgeDocumentResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        KnowledgeDocumentResponse response = documentService.uploadDocument(file);
        return Result.success(response);
    }

    @GetMapping("/list")
    public Result<List<KnowledgeDocumentResponse>> listDocuments() {
        List<KnowledgeDocumentResponse> documents = documentService.listDocuments();
        return Result.success(documents);
    }

    @GetMapping("/{id}/preview")
    public Result<String> getDocumentReviewUrl(@PathVariable("id") Long id) {
        KnowledgeDocumentResponse document = documentService.getDocument(id);
        if (document == null) {
            return Result.error(NOT_FOUND, "文档不存在");
        }
        String presignedUrl = ossTemplate.generateAccessUrl(document.getFileUrl());
        return Result.success(presignedUrl);
    }

    @GetMapping("/{id}")
    public Result<KnowledgeDocumentResponse> getDocument(@PathVariable("id") Long id) {
        KnowledgeDocumentResponse document = documentService.getDocument(id);
        return Result.success(document);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteDocument(@PathVariable("id") Long id) {
        documentService.deleteDocument(id);
        return Result.success();
    }
}
