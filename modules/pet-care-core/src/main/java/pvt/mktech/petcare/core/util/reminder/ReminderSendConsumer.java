package pvt.mktech.petcare.core.util.reminder;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pvt.mktech.petcare.core.dto.message.ReminderExecutionMessageDto;
import pvt.mktech.petcare.core.handler.ReminderWebSocketHandler;
import pvt.mktech.petcare.core.service.ReminderExecutionService;

import static pvt.mktech.petcare.core.constant.CoreConstant.*;

/**
 * 提醒发送服务消费者
 * 监听主题：PET_REMINDER_SEND
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderSendConsumer {

    private final ReminderExecutionService reminderExecutionService;
    private final ReminderWebSocketHandler reminderWebSocketHandler;

    @KafkaListener(topics = CORE_REMINDER_DELAY_TOPIC_SEND, groupId = CORE_REMINDER_SEND_CONSUMER,
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(@Payload String message, @Header(KafkaHeaders.RECEIVED_KEY) String key, Acknowledgment acknowledgment) {
        try {
            ReminderExecutionMessageDto messageDto = JSONUtil.toBean(message, ReminderExecutionMessageDto.class);
            processMessage(messageDto);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("消费消息失败，key: {}", key, e);
            // Kafka 会自动重试，或手动处理
        }
    }

    private void processMessage(ReminderExecutionMessageDto messageDto) {
        Long executionId = messageDto.getId();
        log.info("收到待发送提醒消息，执行ID: {}", executionId);

        try {
            // 4. 调用推送服务（APP推送、短信等）
            log.info("开始推送提醒，执行ID: {}", executionId);
            reminderWebSocketHandler.sendReminderToUser(messageDto.getUserId(), messageDto);
            // 5.更新执行记录状态
            boolean updatedResult = reminderExecutionService.updateSendStatusById(executionId);
            log.info("提醒执行 更新为已发送，执行ID: {}", executionId);
        } catch (Exception e) {
            log.error("推送处理发生异常，执行ID: {}", executionId, e);
            // 异常处理：标记为发送失败，避免消息被重复消费
            // 根据业务需要，可以选择抛出异常让消息重试
            // throw new RuntimeException("推送处理失败", e);
        }
    }
}
