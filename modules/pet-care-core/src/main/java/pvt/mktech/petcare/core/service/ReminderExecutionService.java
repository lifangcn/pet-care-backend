package pvt.mktech.petcare.core.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import pvt.mktech.petcare.core.dto.request.ReminderQueryRequest;
import pvt.mktech.petcare.core.dto.response.ReminderExecutionResponse;
import pvt.mktech.petcare.core.entity.ReminderExecution;

/**
 * 提醒执行记录表 服务层。
 *
 * @author PetCare Code Generator
 * @since 2025-12-22
 */
public interface ReminderExecutionService extends IService<ReminderExecution> {

    /**
     * 根据主键，更新发送状态
     *
     * @param id 主键ID
     * @return
     */
    boolean updateSendStatusById(Long id);


    /**
     * 根据主键，更新读取状态
     *
     * @param id 主键ID
     * @return
     */
    Boolean updateReadStatusById(Long id);

    /**
     * 根据用户ID，更新读取状态
     *
     * @param userId 用户ID
     * @return
     */
    Boolean updateReadStatusByUserId(Long userId);

    /**
     * 更新完成状态
     *
     * @param id              主键ID
     * @param completionNotes
     * @return
     */
    Boolean updateCompleteStatusById(Long id, String completionNotes);

    /**
     * 分页查询执行项
     *
     * @param pageNumber
     * @param pageSize
     * @param request
     * @return
     */
    Page<ReminderExecutionResponse> pageReminderExecutionResponse(Long pageNumber, Long pageSize, ReminderQueryRequest request);

}
