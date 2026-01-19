package pvt.mktech.petcare.club.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pvt.mktech.petcare.club.entity.Label;
import pvt.mktech.petcare.club.entity.Post;
import pvt.mktech.petcare.club.service.LabelService;
import pvt.mktech.petcare.common.dto.response.Result;

import java.util.List;

/**
 * 标签 控制层。
 */
@Tag(name = "标签管理", description = "标签查询、推荐相关接口")
@RestController
@RequestMapping("/label")
@RequiredArgsConstructor
public class LabelController {

    private final LabelService labelService;

    @GetMapping
    @Operation(summary = "标签列表")
    public Result<List<Label>> getLabelList(@RequestParam(required = false) Integer type) {
        return Result.success(labelService.getLabelList(type));
    }

    @GetMapping("/hot")
    @Operation(summary = "热门标签")
    public Result<List<Label>> getHotLabels() {
        return Result.success(labelService.getHotLabels());
    }

    @GetMapping("/suggest")
    @Operation(summary = "标签建议")
    public Result<List<Label>> suggestLabels(@RequestParam String keyword) {
        return Result.success(labelService.suggestLabels(keyword));
    }

    @GetMapping("/posts/{labelId}")
    @Operation(summary = "按标签获取动态")
    public Result<List<Post>> getPostsByLabel(@PathVariable Long labelId) {
        // TODO 扩展点：通过 tb_post_tag 关联查询
        // 这里简化实现，返回空列表
        return Result.success(List.of());
    }
}
