package pvt.mktech.petcare.chat.contentsearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.chat.dto.SearchResult;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Normalizes AI content-search input before delegating it to the selected backend. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentSearchService {
    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 10;
    private static final int MAX_QUERY_CODE_POINTS = 64;

    private final ContentSearchBackend backend;
    @Value("${petcare.search.content-backend}")
    private String backendName;
    @Value("${petcare.search.business-zone:Asia/Shanghai}")
    private String businessZone;

    public List<SearchResult> searchPosts(String query, Integer topK) {
        return execute(query, topK, null, null, false);
    }

    public List<SearchResult> searchActivities(String query, Integer topK, String startTime, String endTime) {
        return execute(query, topK, startTime, endTime, true);
    }

    private List<SearchResult> execute(String rawQuery, Integer requestedTopK, String start, String end, boolean activity) {
        String normalized = normalizeQuery(rawQuery);
        if (normalized.isEmpty()) return List.of();
        try {
            ContentSearchQuery query = new ContentSearchQuery(normalized, "%" + escapeLike(normalized) + "%",
                    escapeLike(normalized) + "%", normalizeTopK(requestedTopK),
                    activity ? parseTime(start) : null, activity ? parseTime(end) : null);
            return activity ? backend.searchActivities(query) : backend.searchPosts(query);
        } catch (Exception exception) {
            log.error("内容检索失败: backend={}, query={}", backendName, normalized, exception);
            return List.of();
        }
    }

    static String normalizeQuery(String query) {
        if (query == null) return "";
        String normalized = query.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        return normalized.codePointCount(0, normalized.length()) <= MAX_QUERY_CODE_POINTS ? normalized : "";
    }

    static String escapeLike(String query) {
        return query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public static int normalizeTopK(Integer topK) {
        return topK == null || topK <= 0 ? DEFAULT_TOP_K : Math.min(topK, MAX_TOP_K);
    }

    private LocalDateTime parseTime(String value) {
        if (value == null || value.isBlank()) return null;
        ZoneId zone = ZoneId.of(businessZone);
        try {
            return OffsetDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME).atZoneSameInstant(zone).toLocalDateTime();
        } catch (Exception ignored) {
            try {
                return ZonedDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME).withZoneSameInstant(zone).toLocalDateTime();
            } catch (Exception ignoredAgain) {
                return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        }
    }
}
