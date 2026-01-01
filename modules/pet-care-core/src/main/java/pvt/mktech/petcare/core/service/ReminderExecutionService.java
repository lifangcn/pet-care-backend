package pvt.mktech.petcare.core.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import pvt.mktech.petcare.core.dto.request.CompleteReminderRequest;
import pvt.mktech.petcare.core.dto.request.ReminderQueryRequest;
import pvt.mktech.petcare.core.entity.Reminder;
import pvt.mktech.petcare.core.entity.ReminderExecution;

/**
 * 提醒执行记录表 服务层。
 *
 * @author PetCare Code Generator
 * @since 2025-12-22
 */
public interface ReminderExecutionService extends IService<ReminderExecution> {

    boolean updateSendStatusById(Long id);

    Boolean updateReadStatusById(Long id);

    Boolean updateCompleteStatusById(Long id, CompleteReminderRequest request);

    Page<ReminderExecution> pageReminderExecution(ReminderQueryRequest request);
}
