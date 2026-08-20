package pvt.mktech.petcare.chat.contentsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ElasticsearchContentSearchBackendTest {
    private final ElasticsearchClient client = mock(ElasticsearchClient.class);
    private final ElasticsearchContentSearchBackend backend = new ElasticsearchContentSearchBackend(client);

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void retainsLegacyBm25ResultMapping() throws Exception {
        SearchResponse<Map> response = mock(SearchResponse.class);
        HitsMetadata<Map> hits = mock(HitsMetadata.class);
        Hit<Map> hit = mock(Hit.class);
        when(hit.source()).thenReturn(Map.of("id", 7L, "title", "Dog food", "content", "review", "like_count", 3));
        when(hit.score()).thenReturn(0.8);
        when(hits.hits()).thenReturn(List.of(hit));
        when(response.hits()).thenReturn(hits);
        when(client.search(org.mockito.ArgumentMatchers.<co.elastic.clients.elasticsearch.core.SearchRequest>any(),
                org.mockito.ArgumentMatchers.eq(Map.class))).thenReturn((SearchResponse) response);

        var results = backend.searchPosts(new ContentSearchQuery("dog food", "%dog food%", "dog food%", 3, null, null));

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.getSource()).isEqualTo("post");
            assertThat(result.getContent()).isEqualTo("review");
            assertThat(result.getMetadata()).containsEntry("id", 7L).doesNotContainKeys("title", "content");
        });
        verify(client).search(org.mockito.ArgumentMatchers.<co.elastic.clients.elasticsearch.core.SearchRequest>any(),
                org.mockito.ArgumentMatchers.eq(Map.class));
    }

    @Test
    void usesExplicitBackendConditions() {
        assertThat(ElasticsearchContentSearchBackend.class.getAnnotation(ConditionalOnProperty.class).havingValue()).isEqualTo("elasticsearch");
        assertThat(PostgresqlContentSearchBackend.class.getAnnotation(ConditionalOnProperty.class).havingValue()).isEqualTo("postgresql");
    }
}
