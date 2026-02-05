package pvt.mktech.petcare.infrastructure.constant;

/**
 * {@code @description}: 常量类
 * {@code @date}: 2025/12/2 11:47
 *
 * @author Michael
 */
public class AiConstant {

    // CDC 消息队列相关
    public static final String PETCARE_MYSQL_CORE_POST_CDC_TOPIC = "PETCARE_MYSQL_CDC.pet_care_ai.tb_post";
    public static final String PETCARE_MYSQL_CORE_ACTIVITY_CDC_TOPIC = "PETCARE_MYSQL_CDC.pet_care_ai.tb_activity";

    public static final String PETCARE_MYSQL_CDC_CONSUMER = "petcare_mysql_cdc_consumer";

}