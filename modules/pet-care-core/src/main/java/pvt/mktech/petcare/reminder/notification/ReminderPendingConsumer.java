package pvt.mktech.petcare.reminder.notification;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pvt.mktech.petcare.reminder.dto.message.ReminderExecutionMessageDto;
import pvt.mktech.petcare.reminder.dto.message.ReminderMessageDto;
import pvt.mktech.petcare.reminder.entity.Reminder;
import pvt.mktech.petcare.reminder.entity.ReminderExecution;
import pvt.mktech.petcare.reminder.service.ReminderExecutionService;
import pvt.mktech.petcare.reminder.service.ReminderService;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static pvt.mktech.petcare.infrastructure.constant.CoreConstant.*;

/**
 * {@code @description}: ReminderPendingConsumer 消费 PENDING Topic，创建提醒执行记录并发送延迟消息
 * {@code @date}: 2025/12/24 16:37
 * @author Michael
 */
@Slf4j
@Component
public class ReminderPendingConsumer implements ApplicationListener<ApplicationReadyEvent> {

    @Resource
    private ReminderService reminderService;
    @Resource
    private DefaultMQProducer defaultMQProducer;
    @Resource
    private ReminderExecutionService reminderExecutionService;

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Value("${rocketmq.consumer.consume-thread-min:1}")
    private Integer consumeThreadMin;

    @Value("${rocketmq.consumer.consume-thread-max:1}")
    private Integer consumeThreadMax;

    private DefaultMQPushConsumer consumer;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            consumer = new DefaultMQPushConsumer(CORE_REMINDER_PENDING_CONSUMER);
            consumer.setNamesrvAddr(nameServer);
            consumer.subscribe(CORE_REMINDER_PENDING_TOPIC, "*");
            consumer.setConsumeThreadMin(consumeThreadMin);
            consumer.setConsumeThreadMax(consumeThreadMax);
            consumer.registerMessageListener(new MessageListenerConcurrently() {
                @Override
                public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                    for (MessageExt msg : msgs) {
                        try {
                            String body = new String(msg.getBody(), StandardCharsets.UTF_8);
                            processMessage(body);
                        } catch (Exception e) {
                            log.error("消息处理失败，msgId: {}, messageBody: {}", msg.getMsgId(), msg.getBody(), e);
                            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                        }
                    }
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
            });
            consumer.start();
            log.info("ReminderPendingConsumer 启动成功");
        } catch (Exception e) {
            log.error("ReminderPendingConsumer 启动失败", e);
            throw new RuntimeException("ReminderPendingConsumer 启动失败", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (consumer != null) {
            consumer.shutdown();
        }
    }

    /**
     * 处理消息：创建提醒执行、更新提醒、发送延迟消息
     * 使用本地事务保证 DB 操作一致性
     */
    @Transactional(rollbackFor = Exception.class)
    public void processMessage(String message) {
        log.info("消费 延迟消费队列 开始，消息内容: {}", message);
        ReminderMessageDto messageDto = JSONUtil.toBean(message, ReminderMessageDto.class);

        // 1. 复核状态
        Reminder reminder = reminderService.getById(messageDto.getId());
        if (reminder == null || !reminder.getIsActive()) {
            log.warn("提醒已停用、不存在或状态不符合, 丢弃 reminder.id: {}", messageDto.getId());
            return;
        }

        // 2. 创建提醒执行记录
        ReminderExecution execution = createExecutionByReminder(messageDto);

        // 3. 更新 Reminder 的 executionId
        reminderService.updateReminderExecutionId(execution.getId(), messageDto.getId());

        // 4. 计算延迟时间并发送延迟消息
        sendDelayMessage(execution);
    }

    /**
     * 创建提醒执行记录，计算通知时间
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

    /**
     * 发送延迟消息
     */
    private void sendDelayMessage(ReminderExecution execution) {
        long delayMillis = Duration.between(LocalDateTime.now(), execution.getNotificationTime()).toMillis();
        log.info("当前时间:{}, 提醒时间：{}, 时间差：{}ms", LocalDateTime.now(), execution.getNotificationTime(), delayMillis);

        try {
            ReminderExecutionMessageDto messageDto = new ReminderExecutionMessageDto();
            BeanUtil.copyProperties(execution, messageDto);
            String body = JSONUtil.toJsonStr(messageDto);

            Message message = new Message(
                    CORE_REMINDER_SEND_TOPIC,
                    body.getBytes(StandardCharsets.UTF_8)
            );

            if (delayMillis > 0) {
                // 未到期，设置延迟时间（RocketMQ 5.x 支持任意毫秒级延迟）
                message.setDelayTimeMs(delayMillis);
                log.info("发送延迟消息，execution.id: {}，延迟时间：{}ms", execution.getId(), delayMillis);
            } else {
                log.info("发送普通消息，execution.id: {}", execution.getId());
            }
            defaultMQProducer.send(message);
        } catch (Exception e) {
            log.error("发送消息失败，execution.id: {}", execution.getId(), e);
            throw new RuntimeException("发送消息失败", e);
        }
    }
}
