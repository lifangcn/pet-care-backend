package pvt.mktech.petcare.sync.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.sync.constants.EsIndexConstants;
import pvt.mktech.petcare.sync.converter.ActivityDocumentConverter;
import pvt.mktech.petcare.sync.dto.event.ActivityCdcData;
import pvt.mktech.petcare.sync.service.CdcHandlerService;

/**
 * {@code @description}: Activity CDC 监听器
 * <p>消费 Debezium 发送的 tb_activity 变更事件，同步到 ES</p>
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityCdcListener {

    private final CdcHandlerService cdcHandlerService;
    private final ActivityDocumentConverter documentConverter;

    private static final String TOPIC_ACTIVITY = "PET_CARE_CDC.pet_care_core.tb_activity";

    /**
     * 监听 Activity 表变更
     */
    @KafkaListener(topics = TOPIC_ACTIVITY, groupId = "es-sync-group")
    public void onActivityChange(ConsumerRecord<String, String> record, Acknowledgment ack) {
        cdcHandlerService.handleCdcEvent(
                record,
                ActivityCdcData.class,
                documentConverter,
                EsIndexConstants.ACTIVITY_INDEX,
                ack
        );
    }
}
