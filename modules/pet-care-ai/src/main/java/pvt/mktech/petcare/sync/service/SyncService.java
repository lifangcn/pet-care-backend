package pvt.mktech.petcare.sync.service;

/**
 * {@code @description}: 数据同步服务接口
 * {@code @date}: 2026-01-30
 * @author Michael
 */
public interface SyncService {

    /**
     * 全量更新文档（覆盖模式）
     */
    <T> void upsert(String index, String id, T document);

    /**
     * 部分更新文档（字段级更新）
     */
    <T> void partialUpdate(String index, String id, T partialDoc);

    /**
     * 删除文档
     */
    void delete(String index, String id);
}
