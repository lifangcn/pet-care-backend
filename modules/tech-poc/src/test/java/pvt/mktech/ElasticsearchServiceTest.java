package pvt.mktech;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.Assert;


/**
 * {@code @description}:
 * {@code @date}: 2025/12/5 17:02
 *
 * @author Michael
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ElasticsearchServiceTest {

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @BeforeEach
    public void setUp() {
        // 清理测试数据
        elasticsearchTemplate.execute(client -> {
            try {
                client.indices().delete(d -> d.index("*test*"));
            } catch (Exception e) {
                // 忽略索引不存在的异常
            }
            return null;
        });
        System.out.println("清理数据");
    }

    @Test
    public void testCreateIndexWithMapping() {
        String indexName = "test_index";

        // 创建索引
        elasticsearchService.createIndexWithMapping(indexName, null);

        // 验证索引创建成功
        Boolean exists = elasticsearchTemplate.execute(client ->
                client.indices().exists(e -> e.index(indexName))).value();
        Assert.isTrue(exists, "索引失败");
    }

    @Test
    public void testDeleteIndex() {
        String indexName = "test_index";

        // 先创建索引
        elasticsearchService.createIndexWithMapping(indexName, null);

        // 删除索引
        elasticsearchService.deleteIndex(indexName);

        // 验证索引已删除
        Boolean exists = elasticsearchTemplate.execute(client ->
                client.indices().exists(e -> e.index(indexName))).value();
        assert !exists;
    }

    @Test
    public void testCRUDDocument() {
        String indexName = "crud_test_index";
        String documentId = "1";
        TestData testData = new TestData("test", 25);

        // 创建索引
        elasticsearchService.createIndexWithMapping(indexName, null);

        // 创建文档
        elasticsearchService.createDocument(indexName, documentId, testData);

        // 查询文档
        TestData retrieved = elasticsearchService.getDocument(indexName, documentId, TestData.class);
        assert retrieved != null;
        assert "test".equals(retrieved.getName());
        assert 25 == retrieved.getAge();

        // 更新文档
        TestData updatedData = new TestData("updated", 30);
        elasticsearchService.updateDocument(indexName, documentId, updatedData);

        // 验证更新
        TestData updated = elasticsearchService.getDocument(indexName, documentId, TestData.class);
        assert "updated".equals(updated.getName());
        assert 30 == updated.getAge();

        // 删除文档
        elasticsearchService.deleteDocument(indexName, documentId);

        // 验证删除
        TestData deleted = elasticsearchService.getDocument(indexName, documentId, TestData.class);
        assert deleted == null;
    }

    // 测试数据类
    public static class TestData {
        private String name;
        private int age;

        public TestData() {}

        public TestData(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }
}
