package pvt.mktech.petcare.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.dto.request.ReminderQueryRequest;
import pvt.mktech.petcare.entity.Reminder;
import pvt.mktech.petcare.mapper.ReminderMapper;
import pvt.mktech.petcare.service.ReminderService;

import static pvt.mktech.petcare.entity.table.ReminderTableDef.REMINDER;

/**
 * 提醒事件表 服务层实现。
 *
 * @author PetCare Code Generator
 * @since 2025-12-22
 */
@Service
public class ReminderServiceImpl extends ServiceImpl<ReminderMapper, Reminder> implements ReminderService {

    @Override
    public Page<Reminder> findPageByQueryRequest(ReminderQueryRequest request) {
        QueryChain<Reminder> queryChain = queryChain().select(REMINDER.ALL_COLUMNS).where(REMINDER.PET_ID.eq(request.getPetId()));
        Page<Reminder> page = new Page<>(request.getPageNumber(), request.getPageSize());
        return page(page, queryChain);
    }

    @Override
    public boolean deactivateById(Long id) {
        boolean update = updateChain().set(REMINDER.IS_ACTIVE, 0).where(REMINDER.ID.eq(id)).update();
        // 从 MQ 中剔除 该提醒
        if (update) {
            // TODO: 2025/12/22 删除 MQ 中的消息
        }
        return update;

    }
}
