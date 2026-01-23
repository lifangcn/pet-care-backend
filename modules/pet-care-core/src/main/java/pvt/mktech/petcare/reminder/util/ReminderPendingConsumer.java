package pvt.mktech.petcare.reminder.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.common.redis.RedisUtil;
import pvt.mktech.petcare.reminder.dto.message.ReminderExecutionMessageDto;
import pvt.mktech.petcare.reminder.dto.message.ReminderMessageDto;
import pvt.mktech.petcare.reminder.entity.Reminder;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.exception.SystemException;
import pvt.mktech.petcare.reminder.entity.ReminderExecution;
import pvt.mktech.petcare.reminder.service.ReminderExecutionService;
import pvt.mktech.petcare.reminder.service.ReminderService;

import java.time.Duration;
import java.time.LocalDateTime;

import static pvt.mktech.petcare.infrastructure.constant.CoreConstant.*;

/**
 * {@code @description}:ReminderPendingConsumer <br/>
 * 2.1.消费 pending topic 时，根据 reminder 生成 execution，核心逻辑是根据 remindBeforeMinutes 计算 notificationTime
 * 2.2.将生成的 execution 根据 notificationTime 和 now() 进行比对。
 * 2.3.如果已经到期，发送到 send queue；
 * 2.4.未到期，存入 Redis ZSet中，等待扫描处理
 * {@code @date}: 2025/12/24 16:37
 *
 * @author Michael
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderPendingConsumer {

    private final ReminderService reminderService;
    private final RedisUtil redisUtil;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ReminderExecutionService reminderExecutionService;

    @KafkaListener(topics = CORE_REMINDER_DELAY_TOPIC_PENDING, groupId = CORE_REMINDER_PENDING_CONSUMER,
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(@Payload String message,
                        @Header(KafkaHeaders.RECEIVED_KEY) String key,
                        Acknowledgment acknowledgment) {
        ReminderMessageDto messageDto = JSONUtil.toBean(message, ReminderMessageDto.class);
        processMessage(messageDto);
        acknowledgment.acknowledge();
    }

    private void processMessage(ReminderMessageDto messageDto) {
        // 1.获取消息并复核状态
        log.info("消费等待提醒消息: {}", messageDto);
        Reminder reminder = reminderService.getById(messageDto.getId());
        if (reminder == null || !reminder.getIsActive()) {
            log.warn("提醒已停用、不存在或状态不符合, 丢弃 reminder.id: {}", messageDto.getId());
            return;
        }
        // 2.创建提醒执行
        ReminderExecution execution = createExecutionByReminder(messageDto);
        // 3.将 提醒执行的主键ID 更新到 提醒 中做一对一关联
        reminderService.updateReminderExecutionId(execution.getId(), messageDto.getId());

        // 4.计算距离提醒时间，决定是否立即发送或者存到Redis队列中
        long delayMillis = Duration.between(LocalDateTime.now(), execution.getNotificationTime()).toMillis();
        log.info("当前时间:{}, 提醒时间：{}, 时间差：{}ms", LocalDateTime.now(), execution.getNotificationTime(), delayMillis);
        // 4.1. 如果已经到期，转发到"发送"主题
        if (delayMillis <= 0) {
            ReminderExecutionMessageDto forwardMessageDto = new ReminderExecutionMessageDto();
            BeanUtil.copyProperties(execution, forwardMessageDto);
            sendToSendQueue(forwardMessageDto);
            return;
        }
        long timestamp = System.currentTimeMillis() + delayMillis;
        // 4.2.如果距离提醒时间还有一段时间，存入 Redis 延迟队列，后续每秒扫描，到期后再处理。
        redisUtil.addToZSet(CORE_REMINDER_SEND_QUEUE_KEY, execution.getId().toString(), timestamp);
        log.info("存入 Redis 延迟队列，execution.id: {}， 消费时间戳：{}", execution.getId(), timestamp);
    }

    private void sendToSendQueue(ReminderExecutionMessageDto messageDto) {
        String key = messageDto.getId().toString();
        String value = JSONUtil.toJsonStr(messageDto);
        kafkaTemplate.send(CORE_REMINDER_DELAY_TOPIC_SEND, key, value)
                .thenAccept(result -> log.info("发送 提醒执行 到立即消费队列，topic: {}, key: {}, body: {}", CORE_REMINDER_DELAY_TOPIC_SEND, key, messageDto))
                .exceptionally(throwable -> {
                    log.error("发送消息到发送队列失败", throwable);
                    throw new SystemException(ErrorCode.MESSAGE_SEND_FAILED, throwable);
        });
    }

    /**
     * 创建提醒执行记录，计算通知时间 (next_trigger_time - 提前量)
     *
     * @param messageDto 消息体
     */
    private ReminderExecution createExecutionByReminder(ReminderMessageDto messageDto) {
        ReminderExecution execution = new ReminderExecution();
        execution.setReminderId(messageDto.getId());
        execution.setPetId(messageDto.getPetId());
        execution.setUserId(messageDto.getUserId());
        execution.setStatus("PENDING");
        execution.setIsSent(Boolean.FALSE);
        execution.setIsRead(Boolean.FALSE);
        execution.setScheduleTime(messageDto.getScheduleTime());
        // 计算通知时间 (next_trigger_time - 提前量)
        Integer remindMinutes = messageDto.getRemindBeforeMinutes();
        LocalDateTime notificationTime = (remindMinutes != null && remindMinutes > 0) ?
                messageDto.getNextTriggerTime().minusMinutes(remindMinutes) :
                messageDto.getNextTriggerTime();
        execution.setNotificationTime(notificationTime);
        reminderExecutionService.save(execution);
        return execution;
    }
}
