package pvt.mktech.petcare.agent.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.chat.dto.SearchResult;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code @description}: 搜索结果重排序工具 - 根据评分/评价对搜索结果重新排序
 * {@code @date}: 2026-03-17
 * @author Michael Li
 */
@Component
public class RerankTool {

    /**
     * 根据评分对搜索结果进行重排序（降序）
     * @param results 原始搜索结果列表
     * @return 按评分降序排列的结果
     */
    @Tool(name = "rerankByRating", description = "根据评分对搜索结果重排序，评分高的排在前面")
    public List<SearchResult> rerankByRating(
            @ToolParam(description = "原始搜索结果列表") List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return results;
        }

        return results.stream()
                .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 根据关键词相关性过滤结果
     * @param results 原始搜索结果
     * @param keyword 关键词
     * @param minScore 最低分数阈值
     * @return 过滤后的结果
     */
    @Tool(name = "filterResultsByKeyword", description = "根据关键词和最低分数过滤搜索结果，只保留包含关键词且分数达标结果")
    public List<SearchResult> filterResultsByKeyword(
            @ToolParam(description = "原始搜索结果列表") List<SearchResult> results,
            @ToolParam(description = "要匹配的关键词") String keyword,
            @ToolParam(description = "最低分数阈值(0-1之间)") Double minScore) {

        if (results == null || results.isEmpty()) {
            return List.of();
        }

        String lowerKeyword = keyword.toLowerCase();
        double scoreThreshold = minScore != null ? minScore : 0.1;

        return results.stream()
                .filter(r -> r.getScore() >= scoreThreshold)
                .filter(r -> containsKeyword(r, lowerKeyword))
                .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 检查结果中是否包含关键词
     */
    private boolean containsKeyword(SearchResult result, String keyword) {
        if (result.getTitle() != null && result.getTitle().toLowerCase().contains(keyword)) {
            return true;
        }
        if (result.getContent() != null && result.getContent().toLowerCase().contains(keyword)) {
            return true;
        }
        return false;
    }
}
