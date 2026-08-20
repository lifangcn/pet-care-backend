package pvt.mktech.petcare.sync.mapper.contentsearch;

import com.mybatisflex.annotation.UseDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import pvt.mktech.petcare.chat.contentsearch.ContentSearchPostRow;
import pvt.mktech.petcare.chat.contentsearch.ContentSearchActivityRow;

import java.time.LocalDateTime;
import java.util.List;

/** PostgreSQL content-search queries against the core datasource. */
@Mapper
@UseDataSource("core")
public interface ContentSearchMapper {
    @Select("""
            SELECT id, user_id, title, content, post_type, location_address, price_range, like_count, rating_avg, created_at,
                   least(1::double precision,
                     .55 * word_similarity(#{query}, lower(coalesce(title, '')))
                     + .35 * word_similarity(#{query}, lower(coalesce(content, '')))
                     + .07 * CASE WHEN lower(coalesce(title, '')) LIKE #{pattern} ESCAPE '\\' THEN 1 ELSE 0 END
                     + .03 * CASE WHEN lower(coalesce(title, '')) LIKE #{prefixPattern} ESCAPE '\\' THEN 1 ELSE 0 END) AS score
            FROM tb_post
            WHERE lower(coalesce(title, '') || ' ' || coalesce(content, '')) LIKE #{pattern} ESCAPE '\\'
              AND enabled = 1 AND audit_status = 'APPROVED' AND is_deleted = false
              AND post_type IN ('PRODUCT', 'SERVICE', 'LOCATION', 'DAILY')
            ORDER BY score DESC, created_at DESC, id DESC
            LIMIT #{topK}
            """)
    List<ContentSearchPostRow> searchPosts(@Param("query") String query, @Param("pattern") String pattern,
                                            @Param("prefixPattern") String prefixPattern, @Param("topK") int topK);

    @Select("""
            SELECT id, user_id, title, description, address, activity_time, status, created_at,
                   least(1::double precision,
                     .50 * word_similarity(#{query}, lower(coalesce(title, '')))
                     + .30 * word_similarity(#{query}, lower(coalesce(description, '')))
                     + .15 * word_similarity(#{query}, lower(coalesce(address, '')))
                     + .03 * CASE WHEN lower(coalesce(title, '')) LIKE #{pattern} ESCAPE '\\' THEN 1 ELSE 0 END
                     + .02 * CASE WHEN lower(coalesce(title, '')) LIKE #{prefixPattern} ESCAPE '\\' THEN 1 ELSE 0 END) AS score
            FROM tb_activity
            WHERE lower(coalesce(title, '') || ' ' || coalesce(description, '') || ' ' || coalesce(address, '')) LIKE #{pattern} ESCAPE '\\'
              AND audit_status = 'APPROVED' AND is_deleted = false
              AND status IN ('RECRUITING', 'ONGOING')
              AND (CAST(#{startTime} AS timestamp) IS NULL OR activity_time >= #{startTime})
              AND (CAST(#{endTime} AS timestamp) IS NULL OR activity_time <= #{endTime})
            ORDER BY score DESC, created_at DESC, id DESC
            LIMIT #{topK}
            """)
    List<ContentSearchActivityRow> searchActivities(@Param("query") String query, @Param("pattern") String pattern,
                                                      @Param("prefixPattern") String prefixPattern, @Param("topK") int topK,
                                                      @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
