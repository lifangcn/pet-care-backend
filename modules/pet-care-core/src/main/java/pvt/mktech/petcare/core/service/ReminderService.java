package pvt.mktech.petcare.core.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import pvt.mktech.petcare.core.dto.request.ReminderQueryRequest;
import pvt.mktech.petcare.core.dto.request.ReminderSaveRequest;
import pvt.mktech.petcare.core.entity.Reminder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提醒事件表 服务层。
 *
 * @author PetCare Code Generator
 * @since 2025-12-22
 */
public interface ReminderService extends IService<Reminder> {

    /**
     * 根据查询条件分页查询提醒事件表。
     *
     * @param request 查询条件
     * @return 分页查询结果
     */
    Page<Reminder> findPageByQueryRequest(ReminderQueryRequest request);

    /**
     * 根据主键，更新提醒事件状态。
     *
     * @param id 主键
     * @return {@code true} 停用成功，{@code false} 停用失败
     */
    boolean updateActiveById(Long id, Boolean isActive);

    List<Reminder> selectRemindersByNextTriggerTime(Boolean isActive, LocalDateTime startTime, LocalDateTime endTime);

    boolean updateNextTriggerTimeById(LocalDateTime nextTriggerTime, Long id);

    boolean updateReminderExecutionId(Long executionId, Long id);
}
