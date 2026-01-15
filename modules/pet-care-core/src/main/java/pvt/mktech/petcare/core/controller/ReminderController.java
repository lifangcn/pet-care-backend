package pvt.mktech.petcare.core.controller;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import pvt.mktech.petcare.common.usercache.UserContext;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.core.dto.request.CompleteReminderRequest;
import pvt.mktech.petcare.core.dto.request.ReminderQueryRequest;
import pvt.mktech.petcare.core.dto.request.ReminderSaveRequest;
import pvt.mktech.petcare.core.dto.response.ReminderExecutionResponse;
import pvt.mktech.petcare.core.entity.Reminder;
import pvt.mktech.petcare.core.entity.ReminderExecution;
import pvt.mktech.petcare.core.service.ReminderExecutionService;
import pvt.mktech.petcare.core.service.ReminderService;

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
    private final ReminderExecutionService reminderExecutionService;

    /**
     * 新增提醒事件表。
     *
     * @param saveRequest 提醒事件请求DTO
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping
    public Result<Boolean> save(@RequestBody ReminderSaveRequest saveRequest) {
        Reminder reminder = new Reminder();
        BeanUtil.copyProperties(saveRequest, reminder);
        reminder.setUserId(UserContext.getUserId());
        // 设置下次提醒时间
        reminder.setNextTriggerTime(reminder.getScheduleTime());
        return Result.success(reminderService.save(reminder));
    }

    /**
     * 根据主键删除提醒事件表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteReminder(@PathVariable("id") Long id) {
        return Result.success(reminderService.removeById(id));
    }

    /**
     * 根据主键 停用提醒
     *
     * @param id          主键
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("/{id}/deactivate")
    public Result<Boolean> deactivate(@PathVariable("id") Long id) {
        return Result.success(reminderService.updateActiveById(id, Boolean.FALSE));
    }

    /**
     * 根据主键 启用提醒
     *
     * @param id 主键
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("/{id}/activate")
    public Result<Boolean> activate(@PathVariable("id") Long id) {
        return Result.success(reminderService.updateActiveById(id, Boolean.TRUE));
    }

    /**
     * 根据主键更新提醒事件表。
     *
     * @param id          主键
     * @param saveRequest 提醒事件对象
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable("id") Long id, @RequestBody ReminderSaveRequest saveRequest) {
        Reminder reminder = new Reminder();
        BeanUtil.copyProperties(saveRequest, reminder);
        reminder.setId(id);
        return Result.success(reminderService.updateById(reminder));
    }

    @PostMapping("/page")
    public Result<Page<Reminder>> pageReminder(@RequestParam(value = "pageNumber", defaultValue = "1") Long pageNumber,
                                               @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize,
                                               @RequestBody ReminderQueryRequest request) {
        request.setUserId(UserContext.getUserId());
        return Result.success(reminderService.findPageByQueryRequest(pageNumber, pageSize, request));
    }

    /* 执行记录处理 */
    @Operation(summary = "完成 提醒执行记录", description = "将提醒执行记录标记为已完成")
    @PutMapping("/execution/{id}/complete")
    public Result<Boolean> complete(@PathVariable("id") Long id) {
        return Result.success(reminderExecutionService.updateCompleteStatusById(id));
    }

    @Operation(summary = "读取 提醒执行记录", description = "将提醒执行记录标记为已读，并填写完成记录")
    @PutMapping("/execution/{id}/read")
    public Result<Boolean> read(@PathVariable("id") Long id) {
        return Result.success(reminderExecutionService.updateReadStatusById(id));
    }

    @Operation(summary = "查询 所有提醒执行记录", description = "根据宠物ID，查询所有提醒执行记录")
    @PostMapping("/execution/page")
    public Result<Page<ReminderExecutionResponse>> pageReminderExecution(@RequestParam(value = "pageNumber", defaultValue = "1") Long pageNumber,
                                                                          @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize,
                                                                          @RequestBody ReminderQueryRequest request) {
        request.setUserId(UserContext.getUserId());
        return Result.success(reminderExecutionService.pageReminderExecutionResponse(pageNumber, pageSize, request));
    }
}
