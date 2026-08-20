package pvt.mktech.petcare.chat.contentsearch;

import java.time.LocalDateTime;

/** Normalized content search input. */
public record ContentSearchQuery(String query, String likePattern, String prefixPattern, int topK,
                                 LocalDateTime startTime, LocalDateTime endTime) {}
