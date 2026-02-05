package pvt.mktech.petcare.sync.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.sync.constants.EsIndexConstants;
import pvt.mktech.petcare.sync.constants.EsIndexMappings;

import java.io.StringReader;

import static pvt.mktech.petcare.sync.constants.SyncConstants.*;

/**
 * {@code @description}: Elasticsearch 索引管理服务
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexAdminService {

    private final ElasticsearchClient elasticsearchClient;

    /**
     * 初始化所有索引
     */
    public void initAllIndices() {
        log.info("开始初始化 Elasticsearch 索引...");
        createKnowledgeDocumentIndex();
        createPostIndex();
        createActivityIndex();

        log.info("Elasticsearch 索引初始化完成");
    }

    /**
     * 创建知识库文档索引
     */
    public boolean createKnowledgeDocumentIndex() {
        return createIndexFromMapping(KNOWLEDGE_DOCUMENT_INDEX, EsIndexMappings.KNOWLEDGE_DOCUMENT_MAPPING);
    }

    /**
     * 创建动态(Post)索引
     */
    public boolean createPostIndex() {
        return createIndexFromMapping(POST_INDEX, EsIndexMappings.POST_MAPPING);
    }

    /**
     * 创建活动(Activity)索引
     */
    public boolean createActivityIndex() {
        return createIndexFromMapping(ACTIVITY_INDEX, EsIndexMappings.ACTIVITY_MAPPING);
    }

    /**
     * 从 JSON 映射字符串创建索引
     *
     * @param indexName   索引名称
     * @param mappingJson 映射 JSON 字符串（包含 settings 和 mappings）
     * @return 是否创建成功
     */
    public boolean createIndexFromMapping(String indexName, String mappingJson) {
        try {
            if (indexExists(indexName)) {
                log.info("索引已存在，跳过创建: {}", indexName);
                return true;
            }

            CreateIndexResponse createIndexResponse = elasticsearchClient.indices().create(builder ->
                    builder.index(indexName).withJson(new StringReader(mappingJson)));

            if (createIndexResponse.acknowledged()) {
                log.info("索引创建成功: {}", indexName);
                return true;
            }
            return false;
        } catch (ElasticsearchException e) {
            if (e.status() == 400 && e.getMessage().contains("resource_already_exists_exception")) {
                log.info("索引已存在: {}", indexName);
                return true;
            }
            log.error("创建索引失败: {}", indexName, e);
            return false;
        } catch (Exception e) {
            log.error("创建索引失败: {}", indexName, e);
            return false;
        }
    }

    /**
     * 检查索引是否存在
     *
     * @param indexName 索引名称
     * @return 是否存在
     */
    public boolean indexExists(String indexName) {
        try {
            return elasticsearchClient.indices()
                    .exists(ExistsRequest.of(e -> e.index(indexName)))
                    .value();
        } catch (Exception e) {
            log.error("检查索引存在性失败: {}", indexName, e);
            return false;
        }
    }

    /**
     * 删除索引
     *
     * @param indexName 索引名称
     * @return 是否删除成功
     */
    public boolean deleteIndex(String indexName) {
        try {
            var response = elasticsearchClient.indices().delete(builder -> builder.index(indexName));
            if (response.acknowledged()) {
                log.info("索引删除成功: {}", indexName);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("删除索引失败: {}", indexName, e);
            return false;
        }
    }
}
