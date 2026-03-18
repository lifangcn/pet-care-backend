package pvt.mktech.petcare.sync.constants;

public final class SyncConstants {

    private SyncConstants() {
    }

    /**
     * 知识库文档索引
     */
    public static final String KNOWLEDGE_DOCUMENT_INDEX = "knowledge_document";

    /**
     * 动态(Post) 索引
     */
    public static final String POST_INDEX = "post";

    /**
     * 活动(Activity) 索引
     */
    public static final String ACTIVITY_INDEX = "activity";

    /**
     * 聊天历史索引
     */
    public static final String CHAT_HISTORY_INDEX = "chat_history";

    /**
     * Agent 执行记录索引
     */
    public static final String AGENT_EXECUTION_INDEX = "agent_execution";

    /**
     * CDC 动态(Post) 主题
     */
    public static final String PET_CARE_CDC_POST_TOPIC = "pet_care_core_tb_post";

    /**
     * CDC 活动(Activity) 主题
     */
    public static final String PET_CARE_CDC_ACTIVITY_TOPIC = "pet_care_core_tb_activity";

    /**
     * CDC 同步消费组名称
     */
    public static final String PET_CARE_CDC_ES_SYNC_CONSUMER_GROUP = "es-sync-group";
}
