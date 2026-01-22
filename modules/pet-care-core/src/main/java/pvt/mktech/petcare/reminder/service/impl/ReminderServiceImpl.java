package pvt.mktech.petcare.reminder.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.reminder.dto.request.ReminderQueryRequest;
import pvt.mktech.petcare.reminder.entity.Reminder;
import pvt.mktech.petcare.reminder.mapper.ReminderMapper;
import pvt.mktech.petcare.reminder.service.ReminderService;

import java.time.LocalDateTime;
import java.util.List;

import static pvt.mktech.petcare.reminder.entity.table.ReminderTableDef.REMINDER;

/**
 * 提醒事件表 服务层实现。
 *
 * @author PetCare Code Generator
 * @since 2025-12-22
 */
@Service
public class ReminderServiceImpl extends ServiceImpl<ReminderMapper, Reminder> implements ReminderService {

    @Override
    public Page<Reminder> findPageByQueryRequest(Long pageNumber, Long pageSize, ReminderQueryRequest request) {
        QueryChain<Reminder> queryChain = queryChain().select(REMINDER.ALL_COLUMNS);

        if (request.getUserId() != null) {
            queryChain.where(REMINDER.USER_ID.eq(request.getUserId()));
        }
        if (request.getPetId() != null) {
            queryChain.and(REMINDER.PET_ID.eq(request.getPetId()));
        }
        if (StrUtil.isNotEmpty(request.getSourceType())) {
            queryChain.and(REMINDER.SOURCE_TYPE.eq(request.getSourceType()));
        }
        if (request.getStartTime() != null) {
            queryChain.and(REMINDER.SCHEDULE_TIME.ge(request.getStartTime()));
        }
        if (request.getEndTime() != null) {
            queryChain.and(REMINDER.SCHEDULE_TIME.le(request.getEndTime()));
        }

        queryChain.orderBy(REMINDER.SCHEDULE_TIME.desc());
        return page(new Page<>(pageNumber, pageSize), queryChain);
    }

    @Override
    public boolean updateActiveById(Long id, Boolean isActive) {
        return updateChain().set(REMINDER.IS_ACTIVE, isActive).where(REMINDER.ID.eq(id)).update();
    }

    @Override
    public List<Reminder> selectRemindersByNextTriggerTime(Boolean isActive, LocalDateTime startTime, LocalDateTime endTime) {
        QueryChain<Reminder> queryChain = queryChain().select(REMINDER.ALL_COLUMNS)
                .where(REMINDER.IS_ACTIVE.eq(isActive))
                .and(REMINDER.NEXT_TRIGGER_TIME.ge(startTime))
                .and(REMINDER.NEXT_TRIGGER_TIME.lt(endTime))
                .orderBy(REMINDER.NEXT_TRIGGER_TIME.asc());
        return queryChain.list();
    }

    @Override
    public boolean updateNextTriggerTimeById(LocalDateTime nextTriggerTime, Long id) {
        return updateChain().set(REMINDER.NEXT_TRIGGER_TIME, nextTriggerTime).where(REMINDER.ID.eq(id)).update();

    }

    @Override
    public boolean updateReminderExecutionId(Long executionId, Long id) {
        return updateChain().set(REMINDER.REMINDER_EXECUTION_ID, executionId).where(REMINDER.ID.eq(id)).update();
    }
}
