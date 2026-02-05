package pvt.mktech.petcare.ai.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest
class ElasticsearchVectorStoreConfigTest {

    @Resource
    private VectorStore elasticsearchVectorStore;

    @Test
    void elasticsearchVectorStore() {
        List<Document> documents = List.of(
                new Document("宠物关怀有什么用啊？新手养宠的知识库，常见小问题用药指南", Map.of("meta1", "meta1")),
                new Document("搬砖工李三的个人项目： petcare.cn —— 宠物关怀系统"),
                new Document("不知道今天晚上吃什么", Map.of("meta2", "meta2")));
        // 添加文档
        elasticsearchVectorStore.add(documents);
        // 相似度查询
        List<Document> results = elasticsearchVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("怎么学养宠啊")
                        .topK(3)
                        .similarityThreshold(0.5)
                        .build());
        Assertions.assertNotNull(results);

    }
}