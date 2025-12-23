package pvt.mktech.petcare.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import pvt.mktech.petcare.dto.request.ReminderQueryRequest;
import pvt.mktech.petcare.entity.Reminder;

/**
 * 提醒事件表 服务层。
 *
 * @author PetCare Code Generator
 * @since 2025-12-22
 */
public interface ReminderService extends IService<Reminder> {

    Page<Reminder> findPageByQueryRequest(ReminderQueryRequest request);

    boolean deactivateById(Long id);
}
