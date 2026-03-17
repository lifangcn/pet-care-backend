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
                  "activity_id": {"type": "long"},
                  "updated_at": {"type": "date"},
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
              "updated_at": {"type": "date"},
              "created_at": {"type": "date"}
            }
          }
        }
        """;

    /**
     * 聊天历史索引映射（支持向量检索）
     */
    public static final String CHAT_HISTORY_MAPPING = """
        {
          "settings": {
            "number_of_shards": 3,
            "number_of_replicas": 1
          },
          "mappings": {
            "properties": {
              "id": {"type": "long"},
              "conversation_id": {"type": "keyword"},
              "user_id": {"type": "long"},
              "session_id": {"type": "keyword"},
              "session_name": {"type": "keyword"},
              "role": {"type": "keyword"},
              "content": {"type": "text", "analyzer": "ik_max_word"},
              "embedding": {
                "type": "dense_vector",
                "dims": 1024,
                "index": true,
                "similarity": "cosine"
              },
              "metadata": {
                "type": "object",
                "properties": {
                  "tool_calls": {"type": "keyword"},
                  "tokens_used": {"type": "integer"},
                  "model": {"type": "keyword"}
                }
              },
              "created_at": {"type": "date"},
              "expires_at": {"type": "date"}
            }
          }
        }
        """;

    /**
     * 聊天链路追踪索引映射
     */
    public static final String CHAT_TRACE_MAPPING = """
        {
          "settings": {
            "number_of_shards": 3,
            "number_of_replicas": 1
          },
          "mappings": {
            "properties": {
              "trace_id": {"type": "keyword"},
              "conversation_id": {"type": "keyword"},
              "session_id": {"type": "keyword"},
              "user_id": {"type": "long"},
              "timestamp": {"type": "date"},
              "duration_ms": {"type": "integer"},
              "request": {
                "type": "object",
                "properties": {
                  "content": {"type": "text"},
                  "tokens": {"type": "integer"}
                }
              },
              "response": {
                "type": "object",
                "properties": {
                  "content": {"type": "text"},
                  "tokens": {"type": "integer"},
                  "finish_reason": {"type": "keyword"}
                }
              },
              "rag": {
                "type": "object",
                "properties": {
                  "enabled": {"type": "boolean"},
                  "query": {"type": "text"},
                  "results_count": {"type": "integer"},
                  "top_score": {"type": "float"},
                  "duration_ms": {"type": "integer"}
                }
              },
              "tool_calls": {
                "type": "nested",
                "properties": {
                  "tool_name": {"type": "keyword"},
                  "arguments": {"type": "text"},
                  "result": {"type": "text"},
                  "duration_ms": {"type": "integer"},
                  "success": {"type": "boolean"}
                }
              },
              "error": {
                "type": "object",
                "properties": {
                  "type": {"type": "keyword"},
                  "message": {"type": "text"},
                  "stack_trace": {"type": "text"}
                }
              },
              "metadata": {
                "type": "object",
                "properties": {
                  "model": {"type": "keyword"},
                  "ip": {"type": "ip"},
                  "user_agent": {"type": "keyword"}
                }
              }
            }
          }
        }
        """;
}
