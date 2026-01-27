package pvt.mktech.petcare.infrastructure.constant;

/**
 * {@code @description}: 常量类
 * {@code @date}: 2025/12/2 11:47
 *
 * @author Michael
 */
public class AiConstant {

    // 消息队列相关(组装消息体逻辑：Topic: core_reminder_pending/send, Tag: pending|send, Key: executionId, Body: ExecutionDto)
    public static final String PETCARE_MYSQL_CORE_KNOWLEDGE_DOCUMENT_CDC_TOPIC = "petcare_mysql.pet_care_ai.tb_knowledge_document";
    public static final String PETCARE_MYSQL_CORE_INTERACTION_CDC_TOPIC = "petcare_mysql.pet_care_ai.tb_interaction";
    public static final String PETCARE_MYSQL_CORE_LABEL_CDC_TOPIC = "petcare_mysql.pet_care_ai.tb_label";
    public static final String PETCARE_MYSQL_CORE_POST_CDC_TOPIC = "petcare_mysql.pet_care_ai.tb_post";
    public static final String PETCARE_MYSQL_CORE_POST_LABEL_CDC_TOPIC = "petcare_mysql.pet_care_ai.tb_post_label";

    public static final String PETCARE_MYSQL_CDC_CONSUMER = "petcare_mysql_cdc_consumer";

}