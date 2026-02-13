package pvt.mktech.petcare.points.mq;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.points.entity.PointsRecord;
import pvt.mktech.petcare.points.mapper.PointsRecordMapper;

import static pvt.mktech.petcare.infrastructure.constant.CoreConstant.CORE_POINTS_RECORD_SAVE_CONSUMER;
import static pvt.mktech.petcare.infrastructure.constant.CoreConstant.CORE_POINTS_RECORD_SAVE_TOPIC;


@Service
@RequiredArgsConstructor
@Slf4j
public class PointsRecordSaveConsumer {

    private final PointsRecordMapper pointsRecordMapper;

    @KafkaListener(topics = CORE_POINTS_RECORD_SAVE_TOPIC, groupId = CORE_POINTS_RECORD_SAVE_CONSUMER,
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(@Payload String message, @Header(KafkaHeaders.RECEIVED_KEY) String key, Acknowledgment acknowledgment) {
        processMessage(message);
        acknowledgment.acknowledge();
    }

    private void processMessage(String jsonString) {
        PointsRecord message = JSONUtil.toBean(jsonString, PointsRecord.class);

        Long bizId = message.getBizId();
        log.info("收到 积分流水保存 消息， 业务主键ID: {}", bizId);

        try {
            // 4. 调用推送服务（APP推送、短信等）
            log.info("开始 保存积分流水，消息内容: {}", jsonString);
            pointsRecordMapper.insert(message);
        } catch (Exception e) {
            log.error("保存积分流水 发生异常", e);
            // 异常处理：标记为发送失败，避免消息被重复消费
            // 根据业务需要，可以选择抛出异常让消息重试
            // throw new RuntimeException("推送处理失败", e);
        }
    }
}
