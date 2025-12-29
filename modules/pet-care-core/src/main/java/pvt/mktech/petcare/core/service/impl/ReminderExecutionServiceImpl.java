package pvt.mktech.petcare.core.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
    public boolean updateSendStatusById(Long reminderExecutionId) {
        return updateChain().set(REMINDER_EXECUTION.STATUS, "COMPLETED")
                .set(REMINDER_EXECUTION.SENT_AT, LocalDateTime.now())
                .set(REMINDER_EXECUTION.IS_SENT, true)
                .where(REMINDER_EXECUTION.ID.eq(reminderExecutionId)).update();
    }
}
