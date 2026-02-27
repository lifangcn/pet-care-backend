package pvt.mktech.petcare.sync.constants;

/**
 * {@code @description}: Elasticsearch 索引映射定义
 * {@code @date}: 2026-01-30
 * @author Michael
 */
public final class EsIndexMappings {

    private EsIndexMappings() {}

    /**
     * 知识库文档索引映射
     */
    public static final String KNOWLEDGE_DOCUMENT_MAPPING = """
            {
              "settings": {
                "number_of_shards": 3,
                "number_of_replicas": 1
              },
              "mappings": {
                "properties": {
                  "id": {"type": "long"},
                  "parent_document_id": {"type": "long"},
                  "name": {"type": "text", "analyzer": "ik_max_word"},
                  "content": {"type": "text", "analyzer": "ik_max_word"},
                  "embedding": {
                    "type": "dense_vector",
                    "dims": 1024,
                    "index": true,
                    "similarity": "cosine"
                  },
                  "chunk_index": {"type": "integer"},
                  "status": {"type": "short"},
                  "created_at": {"type": "date"}
                }
              }
            }
            """;

    /**
     * 动态(Post)索引映射（BM25 全文检索）
     */
    public static final String POST_MAPPING = """
            {
              "settings": {
                "number_of_shards": 3,
                "number_of_replicas": 1
              },
              "mappings": {
                "properties": {
                  "id": {"type": "long"},
                  "user_id": {"type": "long"},
                  "title": {"type": "text", "analyzer": "ik_max_word"},
                  "content": {"type": "text", "analyzer": "ik_max_word"},
                  "post_type": {"type": "text"},
                  "media_urls": {"type": "keyword"},
                  "external_link": {"type": "keyword"},
                  "location_name": {"type": "text"},
                  "location_address": {"type": "text"},
                  "location_latitude": {"type": "double"},
                  "location_longitude": {"type": "double"},
                  "price_range": {"type": "keyword"},
                  "like_count": {"type": "integer"},
                  "rating_avg": {"type": "double"},
                  "rating_count": {"type": "integer"},
                  "rating_total": {"type": "integer"},
                  "view_count": {"type": "integer"},
                  "enabled": {"type": "short"},
                  "status": {"type": "text"},
                  "activity_id": {"type": "long"},
                  "created_at": {"type": "date"}
                }
              }
            }
            """;

    /**
     * 活动(Activity)索引映射（BM25 全文检索）
     */
    public static final String ACTIVITY_MAPPING = """
        {
          "settings": {
            "number_of_shards": 3,
            "number_of_replicas": 1
          },
          "mappings": {
            "properties": {
              "id": {"type": "long"},
              "user_id": {"type": "long"},
              "title": {"type": "text", "analyzer": "ik_max_word"},
              "description": {"type": "text", "analyzer": "ik_max_word"},
              "activity_type": {"type": "text"},
              "activity_time": {"type": "date"},
              "end_time": {"type": "date"},
              "address": {"type": "text", "analyzer": "ik_max_word"},
              "online_link": {"type": "keyword"},
              "max_participants": {"type": "integer"},
              "current_participants": {"type": "integer"},
              "status": {"type": "text"},
              "check_in_enabled": {"type": "short"},
              "check_in_count": {"type": "integer"},
              "created_at": {"type": "date"}
            }
          }
        }
        """;
}
