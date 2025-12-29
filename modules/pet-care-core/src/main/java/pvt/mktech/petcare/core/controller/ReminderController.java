package pvt.mktech.petcare.core.controller;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import pvt.mktech.petcare.common.context.UserContext;
import pvt.mktech.petcare.core.dto.request.ReminderQueryRequest;
import pvt.mktech.petcare.core.dto.request.ReminderSaveRequest;
import pvt.mktech.petcare.core.entity.Reminder;
import pvt.mktech.petcare.core.service.ReminderExecutionService;
import pvt.mktech.petcare.core.service.ReminderService;

import java.time.LocalDateTime;

/**
 * 提醒事件表 控制层。
 *
 * @author PetCare Code Generator
 * @since 2025-12-22
 */
@RestController
@RequestMapping("/reminder")
@Slf4j
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    /**
     * 新增提醒事件表。
     *
     * @param saveRequest 提醒事件请求DTO
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping
    public boolean save(@RequestBody ReminderSaveRequest saveRequest) {
        Reminder reminder = new Reminder();
        BeanUtil.copyProperties(saveRequest, reminder);
        reminder.setUserId(UserContext.getUserInfo().getUserId());
        // 设置下次提醒时间
        reminder.setNextTriggerTime(reminder.getScheduleTime());
        return reminderService.save(reminder);
    }

    /**
     * 根据主键删除提醒事件表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("/{id}")
    public boolean deleteReminder(@PathVariable("id") Long id) {
        return reminderService.removeById(id);
    }

    /**
     * 根据主键 停用提醒
     *
     * @param id          主键
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("/{id}/deactivate")
    public boolean deactivate(@PathVariable("id") Long id) {
        return reminderService.updateActiveById(id, Boolean.FALSE);
    }

    /**
     * 根据主键 启用提醒
     *
     * @param id 主键
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("/{id}/activate")
    public boolean activate(@PathVariable("id") Long id) {
        return reminderService.updateActiveById(id, Boolean.TRUE);
    }

    /**
     * 根据主键更新提醒事件表。
     *
     * @param id          主键
     * @param saveRequest 提醒事件对象
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("/{id}")
    public boolean update(@PathVariable("id") Long id,
                          @RequestBody ReminderSaveRequest saveRequest) {
        Reminder reminder = new Reminder();
        BeanUtil.copyProperties(saveRequest, reminder);
        reminder.setId(id);
        return reminderService.updateById(reminder);
    }

    /**
     * 分页查询提醒事件表。
     *
     * @param petId      宠物ID
     * @param sourceType 提醒来源类型
     * @param startDate  开始时间
     * @param endDate    结束时间
     * @param pageNumber 页码
     * @param pageSize   页大小
     * @return 分页对象
     */
    @GetMapping("/page")
    public Page<Reminder> pageReminder(@RequestParam(value = "petId", required = false) Long petId,
                                       @RequestParam(value = "sourceType", required = false) String sourceType,
                                       @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
                                       @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate,
                                       @RequestParam(value = "pageNumber", defaultValue = "1") Long pageNumber,
                                       @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize) {
        ReminderQueryRequest request = new ReminderQueryRequest(petId, sourceType, startDate, endDate, pageNumber, pageSize);
        return reminderService.findPageByQueryRequest(request);
    }
}
