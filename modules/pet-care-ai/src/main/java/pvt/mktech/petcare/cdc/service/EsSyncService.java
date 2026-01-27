package pvt.mktech.petcare.cdc.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * {@code @description}: ES 同步服务
 * {@code @date}: 2026-01-27
 *
 * @author Michael
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EsSyncService {

    private final ElasticsearchClient elasticsearchClient;

    /**
     * 全量更新文档（覆盖模式）
     */
    public <T> void upsert(String index, String id, T document) {
        try {
            elasticsearchClient.index(i -> i
                    .index(index)
                    .id(id)
                    .document(document));
            log.debug("ES upsert 成功: index={}, id={}", index, id);
        } catch (Exception e) {
            log.error("ES 同步失败: index={}, id={}", index, id, e);
            throw new RuntimeException("ES 同步失败", e);
        }
    }

    /**
     * 部分更新文档（字段级更新）
     */
    public <T> void partialUpdate(String index, String id, T partialDoc) {
        try {
            elasticsearchClient.update(u -> u
                            .index(index)
                            .id(id)
                            .doc(partialDoc)
                            .docAsUpsert(true), partialDoc.getClass());
            log.debug("ES partial update 成功: index={}, id={}", index, id);
        } catch (Exception e) {
            log.error("ES partial update 失败: index={}, id={}", index, id, e);
            throw new RuntimeException("ES partial update 失败", e);
        }
    }

    /**
     * 删除文档
     */
    public void delete(String index, String id) {
        try {
            elasticsearchClient.delete(d -> d.index(index).id(id));
            log.debug("ES delete 成功: index={}, id={}", index, id);
        } catch (Exception e) {
            log.error("ES delete 失败: index={}, id={}", index, id, e);
            throw new RuntimeException("ES delete 失败", e);
        }
    }
}
