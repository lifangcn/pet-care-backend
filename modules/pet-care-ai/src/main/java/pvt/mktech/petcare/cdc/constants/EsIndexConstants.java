package pvt.mktech.petcare.cdc.constants;

/**
 * {@code @description}: Elasticsearch 索引名称常量
 * {@code @date}: 2026-01-27
 * @author Michael
 */
public final class EsIndexConstants {

    private EsIndexConstants() {}

    /** 知识库文档索引 */
    public static final String KNOWLEDGE_DOCUMENT_INDEX = "knowledge_document";

    /** 动态(Post)索引 */
    public static final String POST_INDEX = "post";

    /** 标签(Label)索引 */
    public static final String LABEL_INDEX = "label";

    /** 标签(Activity)索引 */
    public static final String ACTIVITY_INDEX = "activity";

    /** Spring AI 默认向量索引 */
    public static final String VECTOR_STORE_INDEX = "pet-care-vector-store-index";
}
