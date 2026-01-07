package pvt.mktech.petcare.ai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.ai.dto.KnowledgeDocumentResponse;
import pvt.mktech.petcare.ai.service.KnowledgeDocumentService;
import pvt.mktech.petcare.common.util.MinioUtil;

import java.util.List;

import static pvt.mktech.petcare.common.dto.response.ResultCode.NOT_FOUND;

@Slf4j
@RestController
@RequestMapping("/ai/document")
@RequiredArgsConstructor
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService documentService;
    private final MinioUtil minioUtil;

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
        String presignedUrl = minioUtil.generatePreviewUrl(document.getFileUrl());
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

