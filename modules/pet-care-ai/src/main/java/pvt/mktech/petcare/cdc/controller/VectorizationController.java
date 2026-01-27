package pvt.mktech.petcare.cdc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import pvt.mktech.petcare.cdc.service.PostVectorizationService;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code @description}: Post 向量化管理接口
 * {@code @date}: 2026-01-27
 * @author Michael
 */
@Slf4j
@RestController
@RequestMapping("/ai/cdc/vectorization")
@RequiredArgsConstructor
public class VectorizationController {

    private final PostVectorizationService vectorizationService;

    /**
     * 手动触发批量向量化
     */
    @PostMapping("/posts/run")
    public Map<String, Object> vectorizePosts() {
        log.info("手动触发 Post 批量向量化");

        vectorizationService.vectorizePendingPosts();

        return Map.of(
                "code", 200,
                "message", "向量化任务已提交"
        );
    }

    /**
     * 获取向量化状态
     */
    @GetMapping("/posts/status")
    public Map<String, Object> getStatus() {
        // TODO: 查询 ES 中未向量化的数量
        return Map.of(
                "code", 200,
                "data", new HashMap<String, Object>() {{
                    put("pending", 0);
                    put("total", 0);
                }}
        );
    }
}
