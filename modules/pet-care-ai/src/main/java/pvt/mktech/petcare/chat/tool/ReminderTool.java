package pvt.mktech.petcare.chat.tool;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Component
public class ReminderTool {

    @Value("${core.service.url:http://localhost:8080}")
    private String coreServiceUrl;
    @Resource
    private WebClient.Builder webClientBuilder;

    @Tool(name = "设置宠物提醒事项服务")
    public String addReminderFunction(AddReminderRequest request) {
        log.info("制定宠物提醒事项调用成功，请求参数为: {}", request);
        
        try {
            // 转换为 core 服务期望的格式
            ReminderSaveRequest saveRequest = new ReminderSaveRequest();
            saveRequest.setPetName(request.petName());
            saveRequest.setTitle(request.title());
            saveRequest.setDescription(request.description());
            saveRequest.setScheduleTime(request.scheduleTime());
            saveRequest.setRemindBeforeMinutes(request.remindBeforeMinutes());
            saveRequest.setRepeatType(request.repeatType());
            saveRequest.setSourceType("ai-service");
            saveRequest.setRecordTime(LocalDateTime.now());
            saveRequest.setUserId(request.userId());

            String response = webClientBuilder.build()
                    .post()
                    .uri(coreServiceUrl + "/internal/reminder")
                    .bodyValue(saveRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("Core 服务响应: {}", response);
            Boolean result = Boolean.TRUE.equals(response);

            if (Boolean.TRUE.equals(result)) {
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
            @Description("用户ID") Long userId,
            @Description("标题") String title,
            @Description("描述") String description,
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            @Description("预计执行时间，格式：yyyy-MM-dd HH:mm:ss") LocalDateTime scheduleTime,
            @Description("提前提醒时间(分钟)") Integer remindBeforeMinutes,
            @Description("重复类型：'NONE' | 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'CUSTOM'") String repeatType
    ) {}
}
