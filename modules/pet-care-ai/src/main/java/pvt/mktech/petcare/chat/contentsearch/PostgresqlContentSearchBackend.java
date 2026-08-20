package pvt.mktech.petcare.chat.contentsearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.chat.dto.SearchResult;
import pvt.mktech.petcare.sync.mapper.contentsearch.ContentSearchMapper;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

/** PostgreSQL/trgm content-search backend. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "petcare.search.content-backend", havingValue = "postgresql")
public class PostgresqlContentSearchBackend implements ContentSearchBackend {
    private final ContentSearchMapper mapper;

    @Override
    public List<SearchResult> searchPosts(ContentSearchQuery query) {
        try {
            return mapper.searchPosts(query.query(), query.likePattern(), query.prefixPattern(), query.topK()).stream()
                    .map(row -> SearchResult.builder().source("post").type("post").title(row.getTitle()).content(row.getContent())
                            .score(row.getScore()).metadata(postMetadata(row)).build()).toList();
        } catch (Exception exception) {
            log.error("内容检索失败: backend=postgresql, query={}", query.query(), exception);
            return List.of();
        }
    }

    @Override
    public List<SearchResult> searchActivities(ContentSearchQuery query) {
        try {
            return mapper.searchActivities(query.query(), query.likePattern(), query.prefixPattern(), query.topK(), query.startTime(), query.endTime()).stream()
                    .map(row -> SearchResult.builder().source("activity").type("activity").title(row.getTitle()).content(row.getDescription())
                            .score(row.getScore()).metadata(activityMetadata(row)).build()).toList();
        } catch (Exception exception) {
            log.error("内容检索失败: backend=postgresql, query={}", query.query(), exception);
            return List.of();
        }
    }

    private Map<String, Object> postMetadata(ContentSearchPostRow row) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", row.getId()); metadata.put("user_id", row.getUserId()); metadata.put("post_type", row.getPostType());
        metadata.put("location_address", row.getLocationAddress()); metadata.put("price_range", row.getPriceRange());
        metadata.put("like_count", row.getLikeCount()); metadata.put("rating_avg", row.getRatingAvg()); metadata.put("created_at", row.getCreatedAt());
        return metadata;
    }

    private Map<String, Object> activityMetadata(ContentSearchActivityRow row) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", row.getId()); metadata.put("user_id", row.getUserId()); metadata.put("activity_time", row.getActivityTime());
        metadata.put("address", row.getAddress()); metadata.put("status", row.getStatus()); metadata.put("created_at", row.getCreatedAt());
        return metadata;
    }
}
