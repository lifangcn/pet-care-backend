package pvt.mktech.petcare.mcpserver.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * {@code @description}:
 * {@code @date}: 2026/1/6 16:38
 *
 * @author Michael
 */
@Service
public class ReminderToolCallback {

    @Tool(name = "查询天气")
    public String addReminderFunction(AddReminderRequest request) {
        return "";
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
