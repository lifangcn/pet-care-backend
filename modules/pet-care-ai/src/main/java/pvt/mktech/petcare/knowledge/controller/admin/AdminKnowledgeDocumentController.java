package pvt.mktech.petcare.knowledge.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.knowledge.dto.response.KnowledgeDocumentResponse;
import pvt.mktech.petcare.knowledge.service.KnowledgeDocumentService;

import java.util.List;

/**
 * 后台知识库文档管理控制器
 *
 * @author Michael Li
 * @since 2026-03-27
 */
@Tag(name = "后台知识库文档管理", description = "后台知识库文档管理相关接口")
@RestController
@RequestMapping("/admin/ai/document")
@RequiredArgsConstructor
public class AdminKnowledgeDocumentController {

    private final KnowledgeDocumentService documentService;

    /**
     * 上传文档
     *
     * @param file 上传的文件
     * @return 文档信息
     * @author Michael Li
     * @since 2026-03-27
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文档")
    public Result<KnowledgeDocumentResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        return Result.success(documentService.uploadDocument(file));
    }

    /**
     * 查询文档列表
     *
     * @return 文档列表
     * @author Michael Li
     * @since 2026-03-27
     */
    @GetMapping("/list")
    @Operation(summary = "查询文档列表")
    public Result<List<KnowledgeDocumentResponse>> listDocuments() {
        return Result.success(documentService.listDocuments());
    }

    /**
     * 删除文档
     *
     * @param id 文档ID
     * @return 是否成功
     * @author Michael Li
     * @since 2026-03-27
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档")
    public Result<Void> deleteDocument(@PathVariable("id") Long id) {
        documentService.deleteDocument(id);
        return Result.success();
    }

    /**
     * 重新索引文档
     *
     * @param id 文档ID
     * @return 是否成功
     * @author Michael Li
     * @since 2026-03-27
     */
    @PostMapping("/{id}/reindex")
    @Operation(summary = "重新索引文档")
    public Result<Void> reindexDocument(@PathVariable("id") Long id) {
        documentService.reindexDocument(id);
        return Result.success();
    }
}
