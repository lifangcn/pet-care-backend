package pvt.mktech.petcare.core.util.reminder;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pvt.mktech.petcare.core.dto.message.ReminderExecutionMessageDto;
import pvt.mktech.petcare.core.handler.ReminderWebSocketHandler;
import pvt.mktech.petcare.core.service.ReminderExecutionService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.List;

import static pvt.mktech.petcare.core.constant.CoreConstant.*;

/**
 * 提醒发送服务消费者
 * 监听主题：PET_REMINDER_SEND
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderSendConsumer {
    @Value("${rocketmq.name-server:127.0.0.1:9876}")
    private String nameServer;

    private DefaultMQPushConsumer consumer;

    private final ReminderExecutionService reminderExecutionService;
    private final ReminderWebSocketHandler reminderWebSocketHandler;

    @PostConstruct
    public void init() throws MQClientException {
        consumer = new DefaultMQPushConsumer(CORE_REMINDER_SEND_CONSUMER);
        consumer.setNamesrvAddr(nameServer);
        consumer.subscribe(CORE_REMINDER_DELAY_TOPIC_SEND, "*");
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> messages, ConsumeConcurrentlyContext context) {
                for (MessageExt msg : messages) {
                    try {
                        String body = new String(msg.getBody());
                        ReminderExecutionMessageDto messageDto = JSONUtil.toBean(body, ReminderExecutionMessageDto.class);
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
        log.info("RocketMQ Consumer started: {}", CORE_REMINDER_SEND_CONSUMER);
    }

    @PreDestroy
    public void destroy() {
        if (consumer != null) {
            consumer.shutdown();
            log.info("RocketMQ Consumer shutdown: {}", CORE_REMINDER_SEND_CONSUMER);
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
