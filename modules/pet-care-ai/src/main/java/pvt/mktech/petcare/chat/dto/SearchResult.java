package pvt.mktech.petcare.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * {@code @description}: 统一检索结果 DTO
 * AI 可理解的标准格式，用于 Function Calling 返回
 * {@code @date}: 2026-02-03
 * @author Michael
 */
@Data
@Builder
public class SearchResult {

    /**
     * 数据来源：knowledge/post/activity
     */
    private String source;

    /**
     * 文档类型：document/post/activity
     */
    private String type;

    /**
     * 标题（post、activity 有，knowledge_document 可能没有）
     */
    private String title;

    /**
     * 内容（所有类型都有）
     * - knowledge_document: 分块内容
     * - post: content 字段
     * - activity: description 字段
     */
    private String content;

    /**
     * 相关性分数
     */
    private Double score;

    /**
     * 元数据（索引特定字段）
     * - knowledge_document: parent_document_id, chunk_index
     * - post: user_id, like_count, rating_avg, location, price_range
     * - activity: activity_time, address, status
     */
    private Map<String, Object> metadata;

    /**
     * 获取展示用的内容格式
     * 如果有标题，返回 [标题]\n内容
     * 否则直接返回内容
     */
    public String getDisplayContent() {
        if (title != null && !title.isEmpty()) {
            return String.format("[%s]\n%s", title, content);
        }
        return content;
    }
}
