package pvt.mktech.petcare.sync.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.sync.converter.PostDocumentConverter;
import pvt.mktech.petcare.sync.dto.event.PostCdcData;
import pvt.mktech.petcare.sync.service.CdcHandlerService;

import static pvt.mktech.petcare.sync.constants.SyncConstants.*;

/**
 * {@code @description}: Post CDC 监听器
 * <p>消费 Canal 发送的 tb_post 变更事件，同步到 ES</p>
 * {@code @date}: 2026-01-30
 *
 * @author Michael
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostCdcListener {

    private final CdcHandlerService cdcHandlerService;
    private final PostDocumentConverter documentConverter;


    /**
     * 监听 Post 表变更
     */
    @KafkaListener(topics = PET_CARE_CDC_POST_TOPIC, groupId = PET_CARE_CDC_ES_SYNC_CONSUMER_GROUP)
    public void onPostChange(ConsumerRecord<String, String> record, Acknowledgment ack) {
        cdcHandlerService.handleCanalEvent(record, PostCdcData.class, documentConverter,
                POST_INDEX, ack
        );
    }
}
