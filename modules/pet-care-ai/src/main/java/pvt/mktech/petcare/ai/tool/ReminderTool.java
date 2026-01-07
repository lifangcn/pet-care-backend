package pvt.mktech.petcare.ai.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.api.dto.ReminderSaveRequest;
import pvt.mktech.petcare.api.service.ReminderDubboService;
import pvt.mktech.petcare.common.context.UserContext;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderTool {

    @DubboReference(
            interfaceClass = ReminderDubboService.class,
            version = "1.0.0",
            check = false
    )
    private ReminderDubboService reminderService;

    @Tool(name = "设置宠物提醒事项服务")
    public String addReminderFunction(AddReminderRequest request) {
        log.info("制定宠物提醒事项调用成功，请求参数为: {}", request);

        try {
            ReminderSaveRequest saveRequest = new ReminderSaveRequest();
            saveRequest.setPetName(request.petName());
            saveRequest.setTitle(request.title());
            saveRequest.setDescription(request.description());
            saveRequest.setScheduleTime(request.scheduleTime());
            saveRequest.setRemindBeforeMinutes(request.remindBeforeMinutes());
            saveRequest.setRepeatType(request.repeatType());
            saveRequest.setSourceType("manual");
            saveRequest.setRecordTime(LocalDateTime.now());
            saveRequest.setIsActive(true);

            if (UserContext.getUserInfo() != null) {
                saveRequest.setUserId(UserContext.getUserInfo().getUserId());
            }

            // TODO  测试模拟用户ID
            saveRequest.setUserId(1L);

            boolean result = reminderService.saveReminder(saveRequest);

            if (result) {
                return "提醒事项设置成功，已保存到数据库";
            } else {
                return "提醒事项设置失败";
            }
        } catch (Exception e) {
            log.error("调用提醒事项服务失败", e);
            return "提醒事项设置失败：" + e.getMessage();
        }
    }

    public record AddReminderRequest(
            @Description("宠物名称") String petName,
            @Description("标题") String title,
            @Description("描述") String description,
            @Description("预计执行时间，格式：yyyy-MM-dd HH:mm") LocalDateTime scheduleTime,
            @Description("提前提醒时间(分钟)") Integer remindBeforeMinutes,
            @Description("重复类型：'none' | 'daily' | 'weekly' | 'monthly' | 'custom'") String repeatType
    ) {
    }
}
