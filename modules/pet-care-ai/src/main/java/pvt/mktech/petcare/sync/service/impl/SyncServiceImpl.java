package pvt.mktech.petcare.sync.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.sync.service.SyncService;

/**
 * {@code @description}: 数据同步服务实现
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncServiceImpl implements SyncService {

    private final ElasticsearchClient elasticsearchClient;

    @Override
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

    @Override
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

    @Override
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
