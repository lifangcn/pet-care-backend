package pvt.mktech.petcare.sync.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.sync.dto.EsPostDocument;
import pvt.mktech.petcare.sync.entity.core.PostEntity;
import pvt.mktech.petcare.sync.util.DateTimeConverter;

import java.util.List;

/**
 * {@code @description}: Post实体转换器
 * <p>将PostEntity转换为EsPostDocument（用于数据迁移）</p>
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostEntityConverter {

    private final ObjectMapper objectMapper;

    /**
     * 将PostEntity转换为EsPostDocument
     */
    public EsPostDocument convert(PostEntity entity) {
        EsPostDocument doc = new EsPostDocument();
        doc.setId(entity.getId());
        doc.setUserId(entity.getUserId());
        doc.setTitle(entity.getTitle());
        doc.setContent(entity.getContent());
        doc.setPostType(entity.getPostType());
        doc.setMediaUrls(parseMediaUrls(entity.getMediaUrls()));
        doc.setExternalLink(entity.getExternalLink());

        doc.setPriceRange(entity.getPriceRange());
        doc.setLikeCount(entity.getLikeCount());
        doc.setRatingAvg(entity.getRatingAvg());
        doc.setViewCount(entity.getViewCount());
        doc.setStatus(entity.getStatus());
        doc.setActivityId(entity.getActivityId());
        doc.setCreatedAt(DateTimeConverter.toInstant(entity.getCreatedAt()));
        return doc;
    }

    /**
     * 解析 media_urls JSON
     */
    private List<String> parseMediaUrls(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            log.warn("解析 media_urls 失败: {}", json, e);
            return null;
        }
    }
}
