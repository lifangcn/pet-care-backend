package pvt.mktech.petcare.chat.contentsearch;

import org.junit.jupiter.api.Test;
import pvt.mktech.petcare.sync.mapper.contentsearch.ContentSearchMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostgresqlContentSearchBackendTest {
    private final ContentSearchMapper mapper = mock(ContentSearchMapper.class);
    private final PostgresqlContentSearchBackend backend = new PostgresqlContentSearchBackend(mapper);

    @Test
    void mapsPostRowsAndUsesBoundQueryArguments() {
        ContentSearchPostRow row = new ContentSearchPostRow();
        row.setId(1L); row.setUserId(2L); row.setTitle(null); row.setContent(null); row.setScore(.5);
        when(mapper.searchPosts("dog", "%dog%", "dog%", 5)).thenReturn(List.of(row));

        var results = backend.searchPosts(new ContentSearchQuery("dog", "%dog%", "dog%", 5, null, null));

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.getTitle()).isNull();
            assertThat(result.getMetadata()).containsEntry("id", 1L).containsKey("rating_avg");
        });
        verify(mapper).searchPosts("dog", "%dog%", "dog%", 5);
    }

    @Test
    void passesActivityTimeBoundsWithoutFallback() {
        LocalDateTime start = LocalDateTime.of(2026, 2, 22, 8, 0);
        LocalDateTime end = start.plusHours(2);
        when(mapper.searchActivities("walk", "%walk%", "walk%", 2, start, end)).thenReturn(List.of());
        assertThat(backend.searchActivities(new ContentSearchQuery("walk", "%walk%", "walk%", 2, start, end))).isEmpty();
        verify(mapper).searchActivities("walk", "%walk%", "walk%", 2, start, end);
    }
}
