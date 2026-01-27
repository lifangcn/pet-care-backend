package pvt.mktech.petcare.reminder.util;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.reminder.dto.message.ReminderExecutionMessageDto;
import pvt.mktech.petcare.reminder.service.ReminderExecutionService;
import pvt.mktech.petcare.infrastructure.SseConnectionManager;

import static pvt.mktech.petcare.infrastructure.constant.CoreConstant.*;

/**
 * 提醒发送服务消费者
 * 监听主题：PET_REMINDER_SEND
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderSendConsumer {

    private final ReminderExecutionService reminderExecutionService;
    private final SseConnectionManager sseConnectionManager;

    @KafkaListener(topics = CORE_REMINDER_SEND_TOPIC, groupId = CORE_REMINDER_SEND_CONSUMER,
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(@Payload String message, @Header(KafkaHeaders.RECEIVED_KEY) String key, Acknowledgment acknowledgment) {
        processMessage(message);
        acknowledgment.acknowledge();
    }

    private void processMessage(String jsonString) {
        ReminderExecutionMessageDto messageDto = JSONUtil.toBean(jsonString, ReminderExecutionMessageDto.class);

        Long executionId = messageDto.getId();
        log.info("收到待发送提醒消息，执行ID: {}", executionId);

        try {
            // 4. 调用推送服务（APP推送、短信等）
            log.info("开始推送提醒，消息内容: {}", jsonString);
            // 5. 用页面实时提醒
            sseConnectionManager.sendMessage(messageDto.getUserId(), jsonString);
            // 6.更新执行记录状态
            boolean updatedResult = reminderExecutionService.updateSendStatusById(executionId);
            // 7.将已经发送的消息存入 Redis，当查看消息时可以获取
            log.info("提醒执行 更新为已发送，执行ID: {}", executionId);
        } catch (Exception e) {
            log.error("推送处理发生异常，执行ID: {}", executionId, e);
            // 异常处理：标记为发送失败，避免消息被重复消费
            // 根据业务需要，可以选择抛出异常让消息重试
            // throw new RuntimeException("推送处理失败", e);
        }
    }
}
