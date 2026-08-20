package pvt.mktech.petcare.chat.contentsearch;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentSearchServiceTest {
    private final ContentSearchBackend backend = mock(ContentSearchBackend.class);
    private final ContentSearchService service = new ContentSearchService(backend);

    ContentSearchServiceTest() {
        ReflectionTestUtils.setField(service, "backendName", "postgresql");
        ReflectionTestUtils.setField(service, "businessZone", "Asia/Shanghai");
    }

    @Test
    void normalizesEscapesAndCapsValidQuery() {
        when(backend.searchPosts(any())).thenReturn(java.util.List.of());
        service.searchPosts("  DOG_% \\ Food  ", 99);

        var query = org.mockito.ArgumentCaptor.forClass(ContentSearchQuery.class);
        verify(backend).searchPosts(query.capture());
        assertThat(query.getValue().query()).isEqualTo("dog_% \\ food");
        assertThat(query.getValue().likePattern()).isEqualTo("%dog\\_\\% \\\\ food%");
        assertThat(query.getValue().prefixPattern()).isEqualTo("dog\\_\\% \\\\ food%");
        assertThat(query.getValue().topK()).isEqualTo(10);
    }

    @Test
    void rejectsEmptyAndOverlongButKeepsOneAndTwoCodePointQueries() {
        service.searchPosts("   ", 1);
        service.searchPosts("x".repeat(65), 1);
        verify(backend, never()).searchPosts(any());

        when(backend.searchPosts(any())).thenReturn(java.util.List.of());
        service.searchPosts("猫", 0);
        service.searchPosts("狗狗", null);
        verify(backend, org.mockito.Mockito.times(2)).searchPosts(any());
        assertThat(ContentSearchService.normalizeTopK(0)).isEqualTo(5);
    }

    @Test
    void convertsOffsetTimeToBusinessZone() {
        when(backend.searchActivities(any())).thenReturn(java.util.List.of());
        service.searchActivities("walk", 5, "2026-02-22T00:00:00Z", "2026-02-22T12:00:00+00:00");

        var query = org.mockito.ArgumentCaptor.forClass(ContentSearchQuery.class);
        verify(backend).searchActivities(query.capture());
        assertThat(query.getValue().startTime()).isEqualTo(LocalDateTime.of(2026, 2, 22, 8, 0));
        assertThat(query.getValue().endTime()).isEqualTo(LocalDateTime.of(2026, 2, 22, 20, 0));
    }
}
