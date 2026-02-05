package pvt.mktech.petcare.sync.constants;

/**
 * {@code @description}: Elasticsearch 索引名称常量
 * {@code @date}: 2026-01-30
 * @author Michael
 */
public final class EsIndexConstants {

    private EsIndexConstants() {}

    /** 知识库文档索引 */
    public static final String KNOWLEDGE_DOCUMENT_INDEX = "knowledge_document";

    /** 动态(Post)索引 */
    public static final String POST_INDEX = "post";

    /** 活动(Activity)索引 */
    public static final String ACTIVITY_INDEX = "activity";
}
