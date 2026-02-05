package pvt.mktech.petcare.sync.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.sync.constants.EsIndexConstants;
import pvt.mktech.petcare.sync.converter.PostDocumentConverter;
import pvt.mktech.petcare.sync.dto.event.PostCdcData;
import pvt.mktech.petcare.sync.service.CdcHandlerService;

/**
 * {@code @description}: Post CDC 监听器
 * <p>消费 Debezium 发送的 tb_post 变更事件，聚合数据后同步到 ES</p>
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostCdcListener {

    private final CdcHandlerService cdcHandlerService;
    private final PostDocumentConverter documentConverter;

    private static final String TOPIC_POST = "PET_CARE_CDC.pet_care_core.tb_post";

    /**
     * 监听 Post 表变更
     */
    @KafkaListener(topics = TOPIC_POST, groupId = "es-sync-group")
    public void onPostChange(ConsumerRecord<String, String> record, Acknowledgment ack) {
        cdcHandlerService.handleCdcEvent(
                record,
                PostCdcData.class,
                documentConverter,
                EsIndexConstants.POST_INDEX,
                ack
        );
    }
}
