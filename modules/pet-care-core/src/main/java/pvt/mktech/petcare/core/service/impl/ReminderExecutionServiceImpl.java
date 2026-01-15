package pvt.mktech.petcare.core.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.core.dto.request.CompleteReminderRequest;
import pvt.mktech.petcare.core.dto.request.ReminderQueryRequest;
import pvt.mktech.petcare.core.dto.response.ReminderExecutionResponse;
import pvt.mktech.petcare.core.entity.Pet;
import pvt.mktech.petcare.core.entity.Reminder;
import pvt.mktech.petcare.core.entity.ReminderExecution;
import pvt.mktech.petcare.core.mapper.ReminderExecutionMapper;
import pvt.mktech.petcare.core.service.ReminderExecutionService;

import java.time.LocalDateTime;

import static pvt.mktech.petcare.core.entity.table.PetTableDef.PET;
import static pvt.mktech.petcare.core.entity.table.ReminderTableDef.REMINDER;
import static pvt.mktech.petcare.core.entity.table.ReminderExecutionTableDef.REMINDER_EXECUTION;

/**
 * 提醒执行记录表 服务层实现。
 *
 * @author PetCare Code Generator
 * @since 2025-12-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderExecutionServiceImpl extends ServiceImpl<ReminderExecutionMapper, ReminderExecution> implements ReminderExecutionService {

    @Override
    public boolean updateSendStatusById(Long id) {
        return updateChain().set(REMINDER_EXECUTION.STATUS, "COMPLETED")
                .set(REMINDER_EXECUTION.SENT_AT, LocalDateTime.now())
                .set(REMINDER_EXECUTION.IS_SENT, true)
                .where(REMINDER_EXECUTION.ID.eq(id)).update();
    }

    @Override
    public Boolean updateReadStatusById(Long id) {
        return updateChain()
                .set(REMINDER_EXECUTION.READ_AT, LocalDateTime.now())
                .set(REMINDER_EXECUTION.IS_READ, true)
                .where(REMINDER_EXECUTION.ID.eq(id)).update();
    }

    @Override
    public Boolean updateCompleteStatusById(Long id) {
        UpdateChain<ReminderExecution> updateChain = updateChain()
                .set(REMINDER_EXECUTION.STATUS, "COMPLETED")
                .where(REMINDER_EXECUTION.ID.eq(id));
        return updateChain.update();
    }

    @Override
    public Page<ReminderExecution> pageReminderExecution(Long pageNumber, Long pageSize, ReminderQueryRequest request) {
        QueryChain<ReminderExecution> queryChain = queryChain().select(REMINDER_EXECUTION.ALL_COLUMNS);

        if (request.getUserId() != null) {
            queryChain.where(REMINDER_EXECUTION.USER_ID.eq(request.getUserId()));
        }
        if (request.getPetId() != null) {
            queryChain.and(REMINDER_EXECUTION.PET_ID.eq(request.getPetId()));
        }
        if (StrUtil.isNotEmpty(request.getStatus())) {
            queryChain.and(REMINDER_EXECUTION.STATUS.eq(request.getStatus()));
        }
        if (request.getStartTime() != null) {
            queryChain.and(REMINDER_EXECUTION.NOTIFICATION_TIME.ge(request.getStartTime()));
        }
        if (request.getEndTime() != null) {
            queryChain.and(REMINDER_EXECUTION.NOTIFICATION_TIME.le(request.getEndTime()));
        }

        queryChain.orderBy(REMINDER_EXECUTION.NOTIFICATION_TIME.desc());
        return page(new Page<>(pageNumber, pageSize), queryChain);
    }

    @Override
    public Page<ReminderExecutionResponse> pageReminderExecutionResponse(Long pageNumber, Long pageSize, ReminderQueryRequest request) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(
                        REMINDER_EXECUTION.ID,
                        REMINDER_EXECUTION.REMINDER_ID,
                        REMINDER_EXECUTION.PET_ID,
                        REMINDER_EXECUTION.SCHEDULE_TIME,
                        REMINDER_EXECUTION.ACTUAL_TIME,
                        REMINDER_EXECUTION.STATUS,
                        REMINDER_EXECUTION.COMPLETION_NOTES,
                        REMINDER_EXECUTION.NOTIFICATION_TIME,
                        REMINDER_EXECUTION.IS_READ,
                        REMINDER_EXECUTION.IS_SENT,
                        REMINDER_EXECUTION.SENT_AT,
                        REMINDER_EXECUTION.READ_AT,
                        REMINDER.TITLE.as("reminderTitle"),
                        REMINDER.DESCRIPTION.as("reminderDescription"),
                        PET.NAME.as("petName"),
                        PET.AVATAR.as("petAvatar")
                )
                .from(REMINDER_EXECUTION)
                .leftJoin(REMINDER).on(REMINDER_EXECUTION.REMINDER_ID.eq(REMINDER.ID))
                .leftJoin(PET).on(REMINDER_EXECUTION.PET_ID.eq(PET.ID))
                .where(REMINDER_EXECUTION.USER_ID.eq(request.getUserId()))
                .orderBy(REMINDER_EXECUTION.NOTIFICATION_TIME.desc());

        if (request.getPetId() != null) {
            queryWrapper.and(REMINDER_EXECUTION.PET_ID.eq(request.getPetId()));
        }
        if (StrUtil.isNotEmpty(request.getStatus())) {
            queryWrapper.and(REMINDER_EXECUTION.STATUS.eq(request.getStatus()));
        }
        if (request.getStartTime() != null) {
            queryWrapper.and(REMINDER_EXECUTION.NOTIFICATION_TIME.ge(request.getStartTime()));
        }
        if (request.getEndTime() != null) {
            queryWrapper.and(REMINDER_EXECUTION.NOTIFICATION_TIME.le(request.getEndTime()));
        }

        return pageAs(new Page<>(pageNumber, pageSize), queryWrapper, ReminderExecutionResponse.class);
    }
}
