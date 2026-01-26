package pvt.mktech.petcare.reminder.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.exception.SystemException;
import pvt.mktech.petcare.common.redis.RedissonLockUtil;
import pvt.mktech.petcare.infrastructure.constant.CoreConstant;
import pvt.mktech.petcare.reminder.dto.message.ReminderMessageDto;
import pvt.mktech.petcare.reminder.entity.Reminder;
import pvt.mktech.petcare.reminder.service.ReminderService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static pvt.mktech.petcare.infrastructure.constant.CoreConstant.CORE_REMINDER_TOPIC_PENDING;

/**
 * {@code @description}: 定时任务类，找出所有激活的、且计划时间在未来一段时间内的提醒
 * {@code @date}: 2025/12/24 15:37
 *
 * @author Michael
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ReminderScanScheduler {
    private final ReminderService reminderService;
    private final RedissonLockUtil redissonLockUtil;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${scheduler.reminder-scan.look-ahead-minutes:30}")
    private Long lookAheadMinutes;
    @Value("${scheduler.reminder-scan.lock-wait-time:3}")
    private Long lockWaitTime;
    @Value("${scheduler.reminder-scan.lock-lease-time:30}")
    private Long lockLeaseTime;
    /**
     * 完整提醒逻辑：<br>
     * 1.ReminderScanScheduler:
     * 1.1.找出所有激活的、未来某一段时间要触发的 reminder，发送到 pending topic 中。
     * 1.2.此时 reminder 已经触发过，更新 nextTriggerTime 。<br>
     * 2.ReminderPendingConsumer
     * 2.1.消费 pending topic 时，根据 reminder 生成 execution，核心逻辑是根据 remindBeforeMinutes 计算 notificationTime
     * 2.2.将生成的 execution 根据 notificationTime 和 now() 进行比对。
     * 2.3.如果已经到期，发送到 send queue；
     * 2.4.未到期，存入 Redis ZSet中，等待扫描处理<br>
     * 3.ReminderDelayQueueWorker：定时扫描，将已到期的 execution 发送到 send topic <br>
     * 4.ReminderSendConsumer：最后 send topic 进行消费：1. 推送提醒消息；2.execution 改为已发送；3.TBD 将 reminder 改为非激活 <br>
     *
     * 补充：为什么要使用 Redis ZSet 作为延迟队列：
     * 1. 时间排序：score存储时间戳，天然按时间有序
     * 2. 范围查询：rangeByScoreWithScores(0, now) 高效获取到期记录
     * 3. 时间复杂度：增删查都是 O(log N)，适合频繁操作
     * 4. 轻量级: 比引入 RabbitMQ 延迟插件、Quartz 等更轻量（RocketMQ延迟等级不精确，不做讨论）
     */
    @Scheduled(cron = "${scheduler.reminder-scan.cron:0 */1 * * * ?}")  // 默认每1分钟执行一次
    public void reminderScanJob() {
        boolean locked = false;
        try {
            locked = redissonLockUtil.tryLock(CoreConstant.REMINDER_SCAN_LOCK_KEY,
                    lockWaitTime, lockLeaseTime, TimeUnit.SECONDS);
            if (locked) {
                log.info("获取分布式锁成功，开始执行提醒扫描");
                // 找出所有激活的、且计划时间在未来一段时间内的提醒
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime endTime = now.plusMinutes(lookAheadMinutes);
                List<Reminder> toTriggerReminderList = reminderService.selectRemindersByNextTriggerTime(Boolean.TRUE, now, endTime);
                log.info("找到 {} 个提醒记录需要生成执行记录", toTriggerReminderList.size());
                for (Reminder reminder : toTriggerReminderList) {
                    // 更新下次提醒事件触发时间和发送消息，存在非原子性的问题
                    // 1.更新下次提醒时间
                    // 1.1.如果是单次提醒，将下次触发事件设置为空
                    if (reminder.getRepeatType() == null || "NONE".equals(reminder.getRepeatType())) {
                        reminderService.updateNextTriggerTimeById(null, reminder.getId());
                    } else {
                        // 1.2.如果是重复提醒，计算并更新"下一次触发时间"，而不改变 schedule_time
                        LocalDateTime originalTime = reminder.getNextTriggerTime();
                        updateNextTriggerTimeOnly(reminder);
                        log.info("更新 Reminder.nextTriggerTime：[ID:{}], before:{}, after:{}",
                                reminder.getId(), originalTime, reminder.getNextTriggerTime()); // 更新后的值
                    }
                    // 2.发送消息到延迟消费队列
                    forwardToPendingQueue(reminder);
                }
            } else {
                log.info("未获取到分布式锁，跳过本次执行");
            }
        } finally {
            if (locked) {
                redissonLockUtil.unlock(CoreConstant.REMINDER_SCAN_LOCK_KEY);
                log.info("释放分布式锁");
            }
        }
    }

    /**
     * 发送消息到 pending 队列
     *
     * @param reminder 提醒项
     */
    private void forwardToPendingQueue(Reminder reminder) {
        ReminderMessageDto messageDto = new ReminderMessageDto();
        BeanUtil.copyProperties(reminder, messageDto);
        try {
            String key = reminder.getId().toString();
            String value = JSONUtil.toJsonStr(messageDto);
            kafkaTemplate.send(CORE_REMINDER_TOPIC_PENDING, key, value).get();
            log.info("发送 提醒项 到延迟消费队列 成功，topic: {}, key: {}, body: {}",
                    CORE_REMINDER_TOPIC_PENDING, key, messageDto);
        } catch (Exception e) {
            log.error("发送 提醒项 到延迟消费队列 失败，reminder.id: {}", reminder.getId(), e);
            throw new SystemException(ErrorCode.MESSAGE_SEND_FAILED, e);
        }
    }

    /**
     * 【核心】仅更新 next_trigger_time，保持 schedule_time 不变
     */
    private void updateNextTriggerTimeOnly(Reminder reminder) {
        // 重复提醒：基于 schedule_time (基准时间) 和 repeat_type 计算下一次
        LocalDateTime baseTime = reminder.getScheduleTime(); // 用户设置的基准时间
        LocalDateTime lastTriggerTime = reminder.getNextTriggerTime(); // 本次触发的时间
        String repeatType = reminder.getRepeatType();

        LocalDateTime nextTime = calculateNextTime(baseTime, lastTriggerTime, repeatType);

        if (nextTime != null && isBeforeRepeatEnd(nextTime, reminder)) {
            // 更新下一次触发时间
            reminderService.updateNextTriggerTimeById(nextTime, reminder.getId());
        } else {
            // 没有下一次了（如达到结束条件），停用提醒
//            reminder.setIsActive(false);
            reminderService.updateNextTriggerTimeById(null, reminder.getId());
        }
    }

    /**
     * 计算下一次触发时间 (基于基准时间和重复规则)
     */
    private LocalDateTime calculateNextTime(LocalDateTime baseTime,
                                            LocalDateTime lastTriggerTime,
                                            String repeatType) {
        // 如果 lastTriggerTime 为空，说明是第一次，返回基准时间
        if (lastTriggerTime == null) {
            return baseTime;
        }

        // 根据重复类型计算
        return switch (repeatType) {
            // 在最后一次触发的时间，根据重复规则进行累加
            case "DAILY" -> lastTriggerTime.plusDays(1);
            case "WEEKLY" -> lastTriggerTime.plusWeeks(1);
            case "MONTHLY" -> lastTriggerTime.plusMonths(1);
            default -> {
                log.warn("未知重复类型: {}", repeatType);
                yield null;
            }
        };
    }

    /**
     * 检查下一次触发时间是否在重复结束条件之前
     * 这里需要根据你的业务逻辑实现（如结束日期、最大次数等）
     */
    private boolean isBeforeRepeatEnd(LocalDateTime nextTime, Reminder reminder) {
        // TODO: 根据你的业务需求实现结束条件检查
        // 1. 按日期结束：nextTime <= endDate
        // 2. 按次数结束：total_occurrences < max_occurrences
        // 3. 永不结束：return true

        return true;
    }
}
