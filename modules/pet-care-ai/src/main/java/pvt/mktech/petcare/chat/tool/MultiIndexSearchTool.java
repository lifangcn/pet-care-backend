package pvt.mktech.petcare.chat.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.chat.contentsearch.ContentSearchService;
import pvt.mktech.petcare.chat.dto.SearchResult;

import java.util.List;

/** 多索引统一检索工具。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MultiIndexSearchTool {

    private final VectorStore vectorStore;
    private final ContentSearchService contentSearchService;

    @Tool(name = "searchKnowledgeDocuments", description = "检索知识库文档，获取宠物医疗、护理、训练等专业知识")
    public List<SearchResult> searchKnowledge(KnowledgeSearchRequest request) {
        int topK = ContentSearchService.normalizeTopK(request.topK());
        try {
            return vectorStore.similaritySearch(SearchRequest.builder().query(request.query()).topK(topK).build()).stream()
                    .map(document -> SearchResult.builder().source("knowledge").type("document")
                            .title(String.valueOf(document.getMetadata().getOrDefault("filename", "知识库文档")))
                            .content(document.getText()).score(document.getScore()).metadata(document.getMetadata()).build())
                    .toList();
        } catch (Exception exception) {
            log.error("知识库向量检索失败: query={}", request.query(), exception);
            return List.of();
        }
    }

    @Tool(name = "searchUserPosts", description = "检索用户动态，获取好物分享、服务推荐、地点推荐等实际经验")
    public List<SearchResult> searchPosts(PostSearchRequest request) {
        return contentSearchService.searchPosts(request.query(), request.topK());
    }

    @Tool(name = "searchActivities", description = "检索宠物活动，获取线上线下聚会、遛狗活动等信息。当用户提到'周末'、'下周'等时间概念时，必须转换为具体的日期范围传入startTime和endTime参数")
    public List<SearchResult> searchActivities(ActivitySearchRequest request) {
        return contentSearchService.searchActivities(request.query(), request.topK(), request.startTime(), request.endTime());
    }

    public record KnowledgeSearchRequest(@Description("查询文本，例如：狗狗发烧怎么办、猫咪疫苗接种时间") String query,
                                         @Description("返回条数，默认5条，最多10条") Integer topK) {}

    public record PostSearchRequest(@Description("查询文本，例如：宠物医院推荐、狗粮品牌") String query,
                                    @Description("返回条数，默认5条，最多10条") Integer topK) {}

    public record ActivitySearchRequest(@Description("查询文本，例如：宠物活动、遛狗聚会") String query,
                                        @Description("返回条数，默认5条，最多10条") Integer topK,
                                        @Description("活动开始时间过滤，ISO格式") String startTime,
                                        @Description("活动结束时间过滤，ISO格式") String endTime) {}
}
