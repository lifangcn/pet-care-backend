package pvt.mktech.petcare.reminder.notification;

import cn.hutool.json.JSONUtil;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.infrastructure.SseConnectionManager;
import pvt.mktech.petcare.reminder.dto.message.ReminderExecutionMessageDto;
import pvt.mktech.petcare.reminder.service.ReminderExecutionService;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static pvt.mktech.petcare.infrastructure.constant.CoreConstant.CORE_REMINDER_SEND_CONSUMER;
import static pvt.mktech.petcare.infrastructure.constant.CoreConstant.CORE_REMINDER_SEND_TOPIC;

/**
 * {@code @description}: 提醒发送服务消费者
 * 监听主题：CORE_REMINDER_SEND
 * {@code @date}: 2025/12/24 15:37
 * @author Michael
 */
@Slf4j
@Service
public class ReminderSendConsumer implements ApplicationListener<ApplicationReadyEvent> {

    @Resource
    private ReminderExecutionService reminderExecutionService;
    @Resource
    private SseConnectionManager sseConnectionManager;

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
            consumer = new DefaultMQPushConsumer(CORE_REMINDER_SEND_CONSUMER);
            consumer.setNamesrvAddr(nameServer);
            consumer.subscribe(CORE_REMINDER_SEND_TOPIC, "*");
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
            log.info("ReminderSendConsumer 启动成功");
        } catch (Exception e) {
            log.error("ReminderSendConsumer 启动失败", e);
            throw new RuntimeException("ReminderSendConsumer 启动失败", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (consumer != null) {
            consumer.shutdown();
        }
    }

    private void processMessage(String jsonString) {
        ReminderExecutionMessageDto messageDto = JSONUtil.toBean(jsonString, ReminderExecutionMessageDto.class);
        Long executionId = messageDto.getId();
        log.info("收到待发送提醒消息，执行ID: {}", executionId);

        try {
            // 1. 推送消息到 SSE
            sseConnectionManager.sendMessage(messageDto.getUserId(), jsonString);
            // 2. 更新执行记录状态
            boolean updatedResult = reminderExecutionService.updateSendStatusById(executionId);
            log.info("提醒执行 更新为已发送，执行ID: {}", executionId);
        } catch (Exception e) {
            log.error("推送处理发生异常，执行ID: {}", executionId, e);
            // 异常处理：标记为发送失败，避免消息被重复消费
            // 根据业务需要，可以选择抛出异常让消息重试
        }
    }
}
