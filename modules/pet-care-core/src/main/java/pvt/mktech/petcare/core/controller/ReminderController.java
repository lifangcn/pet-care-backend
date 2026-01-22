package pvt.mktech.petcare.core.controller;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.common.usercache.UserContext;
import pvt.mktech.petcare.core.dto.request.ReminderExecutionSaveRequest;
import pvt.mktech.petcare.core.dto.request.ReminderQueryRequest;
import pvt.mktech.petcare.core.dto.request.ReminderSaveRequest;
import pvt.mktech.petcare.core.dto.response.ReminderExecutionResponse;
import pvt.mktech.petcare.core.entity.Reminder;
import pvt.mktech.petcare.core.service.ReminderExecutionService;
import pvt.mktech.petcare.core.service.ReminderService;
import pvt.mktech.petcare.core.util.SseConnectionManager;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 提醒事件表 控制层。
 *
 * @author PetCare Code Generator
 * @since 2025-12-22
 */
@RestController
@RequestMapping("/reminder")
@Slf4j
public class ReminderController {

    @Resource
    private ReminderService reminderService;
    @Resource
    private ReminderExecutionService reminderExecutionService;
    @Resource
    private SseConnectionManager sseConnectionManager;
    @Resource
    private ScheduledThreadPoolExecutor sseHeartbeatThreadPoolExecutor;

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
     * @param id 主键
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
    public Result<Boolean> complete(@PathVariable("id") Long id, @RequestBody(required = false) ReminderExecutionSaveRequest request) {
        return Result.success(reminderExecutionService.updateCompleteStatusById(id, request.getCompletionNotes()));
    }

    @Operation(summary = "已读 提醒执行记录", description = "将提醒执行记录标记为已读，并填写完成记录")
    @PutMapping("/execution/{id}/read")
    public Result<Boolean> read(@PathVariable("id") Long id) {
        return Result.success(reminderExecutionService.updateReadStatusById(id));
    }

    @Operation(summary = "全部已读 提醒执行记录", description = "将提醒执行记录标记为已读，并填写完成记录")
    @PutMapping("/execution/read-all")
    public Result<Boolean> readAll() {
        return Result.success(reminderExecutionService.updateReadStatusByUserId(UserContext.getUserId()));
    }

    @Operation(summary = "查询 所有提醒执行记录", description = "根据宠物ID，查询所有提醒执行记录")
    @PostMapping("/execution/page")
    public Result<Page<ReminderExecutionResponse>> pageReminderExecution(@RequestParam(value = "pageNumber", defaultValue = "1") Long pageNumber,
                                                                         @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize,
                                                                         @RequestBody ReminderQueryRequest request) {
        request.setUserId(UserContext.getUserId());
        return Result.success(reminderExecutionService.pageReminderExecutionResponse(pageNumber, pageSize, request));
    }

    @GetMapping(value = "/sse-connect")
    @Operation(summary = "获取通知连接")
    public SseEmitter sseConnect() {
        // TODO 生成的提醒项会存入Redis中，再通过接口访问，返回到前端进行展示
        Long userId = UserContext.getUserId();

        // 3. 创建 SSE Emitter（超时时间 1 小时）
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);

        // 4. 注册连接
        sseConnectionManager.addConnection(userId, emitter);

        // 5. 设置完成和超时回调
        emitter.onCompletion(() -> sseConnectionManager.removeConnection(userId, emitter));
        emitter.onTimeout(() -> sseConnectionManager.removeConnection(userId, emitter));
        emitter.onError((ex) -> sseConnectionManager.removeConnection(userId, emitter));
        // 6. 立即发送一个连接成功消息（可选）
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"status\":\"connected\"}"));
        } catch (Exception e) {
            // 忽略首次发送错误
            log.warn("SSE 连接建立失败: {}", e.getMessage(), e);
        }
        // 6. 启动心跳线程
        startHeartbeat(emitter);
        return emitter;
    }

    private void startHeartbeat(SseEmitter emitter) {
        // 检查线程池状态
        if (sseHeartbeatThreadPoolExecutor.isShutdown() || sseHeartbeatThreadPoolExecutor.isTerminated()) {
            log.warn("SSE 心跳线程池已关闭，无法启动心跳任务");
            return;
        }

        final ScheduledFuture<?>[] heartbeatTaskRef = new ScheduledFuture[1];
        heartbeatTaskRef[0] = sseHeartbeatThreadPoolExecutor.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (Throwable e) {
                // 心跳失败，连接可能已断开，取消任务
                // 使用 debug 级别，避免客户端断开时打印 ERROR 日志
                if (e instanceof java.io.IOException && "Broken pipe".equals(e.getMessage())) {
                    log.info("SSE 客户端已断开: {}", e.getMessage());
                } else {
                    log.info("SSE 心跳发送失败: {}", e.getMessage());
                }
                if (heartbeatTaskRef[0] != null) {
                    heartbeatTaskRef[0].cancel(false);
                }
            }
        }, 30, 30, TimeUnit.SECONDS);

        // 当连接完成/超时/出错时，清理心跳任务
        Runnable cleanup = () -> {
            if (heartbeatTaskRef[0] != null && !heartbeatTaskRef[0].isDone()) {
                heartbeatTaskRef[0].cancel(false);
            }
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError((ex) -> cleanup.run());
    }
}
