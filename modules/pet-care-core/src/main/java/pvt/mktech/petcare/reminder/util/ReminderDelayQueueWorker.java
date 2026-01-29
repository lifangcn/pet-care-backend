package pvt.mktech.petcare.reminder.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.exception.SystemException;
import pvt.mktech.petcare.common.redis.DistributedLock;
import pvt.mktech.petcare.common.redis.RedisUtil;
import pvt.mktech.petcare.reminder.dto.message.ReminderExecutionMessageDto;
import pvt.mktech.petcare.reminder.entity.ReminderExecution;
import pvt.mktech.petcare.reminder.service.ReminderExecutionService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static pvt.mktech.petcare.infrastructure.constant.CoreConstant.*;

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

    private final RedisUtil redisUtil;
    private final ReminderExecutionService reminderExecutionService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 扫描延迟队列并处理到期消息，使用分布式锁避免多实例重复处理
     */
    @Scheduled(cron = "${scheduler.delay-queue-scan.cron:0/5 * * * * ?}")
    @DistributedLock(lockKey = DELAY_QUEUE_SCAN_LOCK_KEY, waitTime = 1000, leaseTime = 4000)
    public void delayQueueScanJob() {
        Collection<ScoredEntry<Object>> expiredMessages = redisUtil.rangeByScoreWithScores(
                CORE_REMINDER_SEND_QUEUE_KEY, 0, System.currentTimeMillis());
        if (CollUtil.isEmpty(expiredMessages)) {
            return;
        }

        log.info("处理 Redis 延迟提醒消息：{} 条", expiredMessages.size());

        for (ScoredEntry<Object> tuple : expiredMessages) {
            String executionId = tuple.getValue().toString();
            Double scheduleTime = tuple.getScore();
            if (executionId == null || scheduleTime == null) {
                log.error("延迟消息格式错误，executionId={}, scheduleTime={}", executionId, scheduleTime);
                continue;
            }
            try {
                log.info("处理到期消息: executionId={}, 计划发送时间={}",
                        executionId, LocalDateTime.ofInstant(Instant.ofEpochMilli(scheduleTime.longValue()),
                                ZoneId.of("Asia/Shanghai")));

                this.forwardToSendQueue(Long.parseLong(executionId));
            } catch (Exception e) {
                log.error("处理延时消息异常，executionId: {}", executionId, e);
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
            redisUtil.removeFromZSet(CORE_REMINDER_SEND_QUEUE_KEY, reminderExecutionId.toString());
            log.warn("执行记录状态不是待发送，取消转发 {} ", execution);
            return; // 返回，让上游从Redis删除，因为业务上已无效
        }
        // 2. 发送到Kafka，由下游的 NotificationService 消费并实际推送
        ReminderExecutionMessageDto messageDto = new ReminderExecutionMessageDto();
        BeanUtil.copyProperties(execution, messageDto);
        String key = execution.getId().toString();
        String value = JSONUtil.toJsonStr(messageDto);
        ProducerRecord<String, String> message = new ProducerRecord<>(CORE_REMINDER_TOPIC_SEND, null, System.currentTimeMillis(), key, value);
        // 3.先从 ZSet 队列中删除
        redisUtil.removeFromZSet(CORE_REMINDER_SEND_QUEUE_KEY, key);
        // 4.发送消息
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(message);
        future.thenAccept(result -> {
                    log.info("成功发送 提醒执行 消息，topic: {}, key: {}, offset: {}", CORE_REMINDER_TOPIC_SEND, key, result.getRecordMetadata().offset());
                })
                .exceptionally(throwable -> {
                    log.error("发送 提醒执行 到立即消费队列失败，topic: {}, key: {}, 异常: {}", CORE_REMINDER_TOPIC_SEND, key, throwable.getMessage());
                    log.error("转发消息到发送队列失败，executionId={} 仍保留在延迟队列中", key);
                    throw new SystemException(ErrorCode.MESSAGE_SEND_FAILED, throwable);
                    // 可以实现重试逻辑或记录到失败队列
                });
    }
}
