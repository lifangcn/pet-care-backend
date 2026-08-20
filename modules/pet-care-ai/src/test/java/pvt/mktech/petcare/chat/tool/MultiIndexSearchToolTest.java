package pvt.mktech.petcare.chat.tool;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import pvt.mktech.petcare.chat.contentsearch.ContentSearchService;
import pvt.mktech.petcare.chat.dto.SearchResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MultiIndexSearchToolTest {

    private final VectorStore vectorStore = mock(VectorStore.class);
    private final ContentSearchService contentSearchService = mock(ContentSearchService.class);
    private final MultiIndexSearchTool tool = new MultiIndexSearchTool(vectorStore, contentSearchService);

    @Test
    void searchKnowledgeUsesDefaultTopKAndMapsVectorDocumentsWithoutElasticsearch() {
        Document document = mock(Document.class);
        when(document.getText()).thenReturn("vaccination content");
        when(document.getMetadata()).thenReturn(Map.of("filename", "vaccine.md", "chunk_index", 0));
        when(document.getScore()).thenReturn(0.92);
        when(vectorStore.similaritySearch(org.mockito.ArgumentMatchers.any(SearchRequest.class))).thenReturn(List.of(document));

        List<SearchResult> results = tool.searchKnowledge(new MultiIndexSearchTool.KnowledgeSearchRequest("vaccination", null));

        ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(request.capture());
        assertThat(request.getValue().getQuery()).isEqualTo("vaccination");
        assertThat(request.getValue().getTopK()).isEqualTo(5);
        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.getTitle()).isEqualTo("vaccine.md");
            assertThat(result.getContent()).isEqualTo("vaccination content");
            assertThat(result.getMetadata()).containsEntry("chunk_index", 0);
            assertThat(result.getScore()).isEqualTo(0.92);
        });
        verifyNoInteractions(contentSearchService);
    }

    @Test
    void searchKnowledgeCapsTopKAtTenAndReturnsEmptyListWhenVectorStoreFails() {
        when(vectorStore.similaritySearch(org.mockito.ArgumentMatchers.any(SearchRequest.class))).thenThrow(new IllegalStateException("offline"));

        assertThat(tool.searchKnowledge(new MultiIndexSearchTool.KnowledgeSearchRequest("query", 99))).isEmpty();

        ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(request.capture());
        assertThat(request.getValue().getTopK()).isEqualTo(10);
        verifyNoInteractions(contentSearchService);
    }

    @Test
    void delegatesPostAndActivitySearchToContentSearchService() {
        List<SearchResult> posts = List.of(SearchResult.builder().source("post").build());
        List<SearchResult> activities = List.of(SearchResult.builder().source("activity").build());
        when(contentSearchService.searchPosts("dog food", 3)).thenReturn(posts);
        when(contentSearchService.searchActivities("meetup", 4, "2026-02-22T00:00:00Z", "2026-02-23T00:00:00Z"))
                .thenReturn(activities);

        assertThat(tool.searchPosts(new MultiIndexSearchTool.PostSearchRequest("dog food", 3))).isSameAs(posts);
        assertThat(tool.searchActivities(new MultiIndexSearchTool.ActivitySearchRequest("meetup", 4,
                "2026-02-22T00:00:00Z", "2026-02-23T00:00:00Z"))).isSameAs(activities);

        verify(contentSearchService).searchPosts("dog food", 3);
        verify(contentSearchService).searchActivities("meetup", 4, "2026-02-22T00:00:00Z", "2026-02-23T00:00:00Z");
        verifyNoInteractions(vectorStore);
    }
}
