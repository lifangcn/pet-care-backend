package pvt.mktech.petcare.cdc.constants;

/**
 * {@code @description}: Elasticsearch 索引映射定义
 * {@code @date}: 2026-01-27
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
                  "name": {"type": "text", "analyzer": "ik_max_word"},
                  "file_url": {"type": "keyword"},
                  "file_type": {"type": "keyword"},
                  "content": {"type": "text", "analyzer": "ik_max_word"},
                  "embedding": {
                    "type": "dense_vector",
                    "dims": 1024,
                    "index": true,
                    "similarity": "cosine"
                  },
                  "chunk_index": {"type": "integer"},
                  "parent_document_id": {"type": "long"},
                  "version": {"type": "integer"},
                  "status": {"type": "short"},
                  "created_at": {"type": "date"}
                }
              }
            }
            """;

    /**
     * 动态(Post)索引映射
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
                  "post_type": {"type": "short"},
                  "media_urls": {"type": "flattened"},
                  "external_link": {"type": "keyword"},
                  "location": {
                    "type": "object",
                    "properties": {
                      "name": {"type": "text"},
                      "address": {"type": "text"},
                      "latitude": {"type": "double"},
                      "longitude": {"type": "double"}
                    }
                  },
                  "price_range": {"type": "keyword"},
                  "like_count": {"type": "integer"},
                  "rating_avg": {"type": "float"},
                  "view_count": {"type": "integer"},
                  "status": {"type": "short"},
                  "activity_id": {"type": "long"},
                  "labels": {"type": "keyword"},
                  "embedding": {
                    "type": "dense_vector",
                    "dims": 1024,
                    "index": true,
                    "similarity": "cosine"
                  },
                  "created_at": {"type": "date"}
                }
              }
            }
            """;

    /**
     * 标签(Label)索引映射
     */
    public static final String LABEL_MAPPING = """
            {
              "settings": {
                "number_of_shards": 1,
                "number_of_replicas": 1
              },
              "mappings": {
                "properties": {
                  "id": {"type": "long"},
                  "name": {
                    "type": "text",
                    "fields": {
                      "keyword": {"type": "keyword"}
                    }
                  },
                  "type": {"type": "short"},
                  "icon": {"type": "keyword"},
                  "color": {"type": "keyword"},
                  "use_count": {"type": "integer"},
                  "is_recommended": {"type": "short"},
                  "status": {"type": "short"},
                  "created_at": {"type": "date"}
                }
              }
            }
            """;
    /**
     * 活动(Activity)索引映射
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
              "activity_type": {"type": "short"},
              "activity_time": {"type": "date"},
              "end_time": {"type": "date"},
              "address": {"type": "text", "analyzer": "ik_max_word"},
              "online_link": {"type": "keyword"},
              "max_participants": {"type": "integer"},
              "current_participants": {"type": "integer"},
              "status": {"type": "short"},
              "labels": {"type": "keyword"},
              "check_in_enabled": {"type": "short"},
              "check_in_count": {"type": "integer"},
              "created_at": {"type": "date"}
            }
          }
        }
        """;
}
