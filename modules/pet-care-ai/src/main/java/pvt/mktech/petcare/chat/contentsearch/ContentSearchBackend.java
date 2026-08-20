package pvt.mktech.petcare.chat.contentsearch;

import pvt.mktech.petcare.chat.dto.SearchResult;

import java.util.List;

/** Content search backend seam. */
public interface ContentSearchBackend {
    List<SearchResult> searchPosts(ContentSearchQuery query);
    List<SearchResult> searchActivities(ContentSearchQuery query);
}
