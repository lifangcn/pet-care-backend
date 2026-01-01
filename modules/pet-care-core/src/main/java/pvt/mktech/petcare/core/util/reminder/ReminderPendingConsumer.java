package pvt.mktech.petcare.core.util.reminder;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.core.dto.message.ReminderExecutionMessageDto;
import pvt.mktech.petcare.core.dto.message.ReminderMessageDto;
import pvt.mktech.petcare.core.entity.Reminder;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.exception.SystemException;
import pvt.mktech.petcare.core.entity.ReminderExecution;
import pvt.mktech.petcare.core.service.ReminderExecutionService;
import pvt.mktech.petcare.core.service.ReminderService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static pvt.mktech.petcare.core.constant.CoreConstant.*;

/**
 * {@code @description}:
 * {@code @date}: 2025/12/24 16:37
 *
 * @author Michael
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderPendingConsumer {

    private final ReminderService reminderService;
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultMQProducer reminderProducer;
    private final ReminderExecutionService reminderExecutionService;

    @Value("${rocketmq.name-server:127.0.0.1:9876}")
    private String nameServer;

    private DefaultMQPushConsumer consumer;

    @PostConstruct
    public void init() throws MQClientException {
        consumer = new DefaultMQPushConsumer(CORE_REMINDER_PENDING_CONSUMER);
        consumer.setNamesrvAddr(nameServer);
        consumer.subscribe(CORE_REMINDER_DELAY_TOPIC_PENDING, "*");
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> messages, ConsumeConcurrentlyContext context) {
                for (MessageExt msg : messages) {
                    try {
                        String body = new String(msg.getBody());
                        ReminderMessageDto messageDto = JSONUtil.toBean(body, ReminderMessageDto.class);
                        processMessage(messageDto);
                    } catch (Exception e) {
                        log.error("消费消息失败，messageId: {}", msg.getMsgId(), e);
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();
        log.info("RocketMQ Consumer started: {}", CORE_REMINDER_PENDING_CONSUMER);
    }

    @PreDestroy
    public void destroy() {
        if (consumer != null) {
            consumer.shutdown();
            log.info("RocketMQ Consumer shutdown: {}", CORE_REMINDER_PENDING_CONSUMER);
        }
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
        log.info("提醒时间：{}, 当前时间:{}, 时间差：{}ms", LocalDateTime.now(), execution.getNotificationTime(), delayMillis);
        // 4.1. 如果已经到期，转发到"发送"主题
        if (delayMillis <= 0) {
            ReminderExecutionMessageDto forwardMessageDto = new ReminderExecutionMessageDto();
            BeanUtil.copyProperties(execution, forwardMessageDto);
            sendToSendQueue(forwardMessageDto);
            return;
        }
        long timestamp = System.currentTimeMillis() + delayMillis;
        // 4.2.如果距离提醒时间还有一段时间，存入 Redis 队列，后续每秒扫描，到期后再处理。
        Boolean flag = stringRedisTemplate.opsForZSet()
                .add(CORE_REMINDER_QUEUE_KEY, execution.getId().toString(), timestamp);
        log.info("存入 Redis 队列，是否成功：{}, execution.id: {}， 消费时间戳：{}", flag, execution.getId(), timestamp);
    }

    private void sendToSendQueue(ReminderExecutionMessageDto messageDto) {
        try {
            Message message = new Message(
                    CORE_REMINDER_DELAY_TOPIC_SEND,
                    messageDto.getId().toString(),
                    JSONUtil.toJsonStr(messageDto).getBytes()
            );
            reminderProducer.send(message);
            log.info("发送 提醒执行 到立即消费队列，topic: {}, body: {}", CORE_REMINDER_DELAY_TOPIC_SEND, messageDto);
        } catch (Exception e) {
            log.error("发送消息到发送队列失败", e);
            throw new SystemException(ErrorCode.MESSAGE_SEND_FAILED, e);
        }
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
