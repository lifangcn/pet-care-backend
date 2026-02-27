package pvt.mktech.petcare.sync.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.sync.dto.EsPostDocument;
import pvt.mktech.petcare.sync.dto.event.PostCdcData;
import pvt.mktech.petcare.sync.util.DateTimeConverter;

import java.time.Instant;
import java.util.List;

/**
 * {@code @description}: Post文档转换器
 * <p>将PostCdcData转换为EsPostDocument</p>
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostDocumentConverter implements DocumentConverter<PostCdcData, EsPostDocument> {

    private final ObjectMapper objectMapper;

    @Override
    public EsPostDocument convert(PostCdcData cdcData) {
        EsPostDocument doc = new EsPostDocument();
        doc.setId(cdcData.getId());
        doc.setUserId(cdcData.getUserId());
        doc.setTitle(cdcData.getTitle());
        doc.setContent(cdcData.getContent());
        doc.setPostType(cdcData.getPostType());
        doc.setMediaUrls(parseMediaUrls(cdcData.getMediaUrls()));
        doc.setExternalLink(cdcData.getExternalLink());
        doc.setLocationAddress(cdcData.getLocationAddress());
        doc.setPriceRange(cdcData.getPriceRange());
        doc.setLikeCount(cdcData.getLikeCount());
        doc.setRatingCount(cdcData.getRatingCount());
        doc.setRatingTotal(cdcData.getRatingTotal());
        doc.setRatingAvg(cdcData.getRatingAvg());
        doc.setViewCount(cdcData.getViewCount());
        doc.setActivityId(cdcData.getActivityId());
        doc.setCreatedAt(DateTimeConverter.parseCanalDateTime(cdcData.getCreatedAt()));
        return doc;
    }

    /**
     * 解析 media_urls JSON
     */
    private List<String> parseMediaUrls(String json) {
        if (json == null || json.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            log.warn("解析 media_urls 失败: {}", json, e);
            return List.of();
        }
    }
}
