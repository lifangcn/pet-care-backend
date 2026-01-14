package pvt.mktech.petcare.core.util.reminder;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.exception.SystemException;
import pvt.mktech.petcare.core.dto.message.ReminderExecutionMessageDto;
import pvt.mktech.petcare.core.entity.ReminderExecution;
import pvt.mktech.petcare.core.service.ReminderExecutionService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static pvt.mktech.petcare.core.constant.CoreConstant.*;

/**
 * {@code @description}: ReminderDelayQueueWorker：定时扫描，将已到期的 execution 发送到 send topic
 * {@code @date}: 2025/12/24 15:37
 *
 * @author Michael
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ReminderDelayQueueWorker {

    private final StringRedisTemplate stringRedisTemplate;
    private final ReminderExecutionService reminderExecutionService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 扫描延迟队列并处理到期消息，
     * TODO: 后续可优化扫描频率和批量处理
     * TODO： 潜在的并发问题 1.Redis ZSet 操作并发：多个实例同时扫描和删除 ZSet 中的消息可能导致数据不一致 2.消息重复消费：在分布式环境下，多个消费者实例可能同时处理相同的消息
     */
    @XxlJob("delayQueueScanJob")
    public void delayQueueScanJob() {
        Set<ZSetOperations.TypedTuple<String>> expiredMessages = stringRedisTemplate.opsForZSet()
                .rangeByScoreWithScores(CORE_REMINDER_SEND_QUEUE_KEY, 0, System.currentTimeMillis());

        if (CollUtil.isEmpty(expiredMessages)) {
            return;
        }

        log.info("处理 Redis 消息：{} 条", expiredMessages.size());

        for (ZSetOperations.TypedTuple<String> tuple : expiredMessages) {
            String executionId = tuple.getValue();
            Double scheduleTime = tuple.getScore();
            if (executionId == null || scheduleTime == null) {
                log.error("延迟消息格式错误，executionId={}, scheduleTime={}", executionId, scheduleTime);
                continue;
            }
            try {
                log.info("处理到期消息: executionId={}, 计划发送时间={}",
                        executionId, LocalDateTime.ofInstant(Instant.ofEpochMilli(scheduleTime.longValue()),
                                ZoneId.of("Asia/Shanghai")));

                // 3. 核心：取出消息后，将其转发到真正的发送队列（Kafka主题）
                this.forwardToSendQueue(Long.parseLong(executionId));
            } catch (Exception e) {
                log.error("处理延时消息异常，executionId: {}", executionId, e);
                // 异常处理：可加入重试机制或告警
            }
        }
    }

    /**
     * 发送消息到 send 队列
     *
     * @param reminderExecutionId 执行记录ID
     */
    private void forwardToSendQueue(Long reminderExecutionId) {
        // 1. 可以根据ID从数据库再查询一次最新状态（二次校验的又一机会）
        ReminderExecution execution = reminderExecutionService.getById(reminderExecutionId);
        if (execution == null || !"pending".equals(execution.getStatus())) {
            log.warn("执行记录 {} 状态不符，取消转发", execution);
            return; // 返回true，让上游从Redis删除，因为业务上已无效
        }
        // 2. 发送到Kafka，由下游的 NotificationService 消费并实际推送
        ReminderExecutionMessageDto messageDto = new ReminderExecutionMessageDto();
        BeanUtil.copyProperties(execution, messageDto);
        String key = execution.getId().toString();
        String value = JSONUtil.toJsonStr(messageDto);
        ProducerRecord<String, String> message = new ProducerRecord<>(CORE_REMINDER_DELAY_TOPIC_SEND, null, System.currentTimeMillis(), key, value);
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(message);
        future.thenAccept(result -> {
                    log.info("成功发送 提醒执行 消息，topic: {}, key: {}, offset: {}", CORE_REMINDER_DELAY_TOPIC_SEND, key, result.getRecordMetadata().offset());
                    // 4. 处理成功后，从Sorted Set中移除该消息，防止重复消费
                    stringRedisTemplate.opsForZSet().remove(CORE_REMINDER_SEND_QUEUE_KEY, key);
                    log.info("成功处理并移除延时消息: executionId={}", key);
                })
                .exceptionally(throwable -> {
                    log.error("发送 提醒执行 到立即消费队列失败，topic: {}, key: {}, 异常: {}", CORE_REMINDER_DELAY_TOPIC_SEND, key, throwable.getMessage());
                    log.error("转发消息到发送队列失败，executionId={} 仍保留在延迟队列中", key);
                    throw new SystemException(ErrorCode.MESSAGE_SEND_FAILED, throwable);
                    // 可以实现重试逻辑或记录到失败队列
                });
    }
}
