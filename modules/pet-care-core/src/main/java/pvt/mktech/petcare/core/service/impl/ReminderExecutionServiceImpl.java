package pvt.mktech.petcare.core.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.core.dto.request.CompleteReminderRequest;
import pvt.mktech.petcare.core.dto.request.ReminderQueryRequest;
import pvt.mktech.petcare.core.entity.ReminderExecution;
import pvt.mktech.petcare.core.mapper.ReminderExecutionMapper;
import pvt.mktech.petcare.core.service.ReminderExecutionService;

import java.time.LocalDateTime;

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
    public Boolean updateCompleteStatusById(Long id, CompleteReminderRequest request) {
        UpdateChain<ReminderExecution> updateChain = updateChain()
                .set(REMINDER_EXECUTION.STATUS, "COMPLETED")
                .where(REMINDER_EXECUTION.ID.eq(id));
        if (StrUtil.isNotBlank(request.getCompletionNotes())) {
            updateChain.set(REMINDER_EXECUTION.COMPLETION_NOTES, request.getCompletionNotes());
        }

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
}
