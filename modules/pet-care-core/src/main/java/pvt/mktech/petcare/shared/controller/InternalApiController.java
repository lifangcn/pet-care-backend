package pvt.mktech.petcare.shared.controller;

import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import pvt.mktech.petcare.reminder.dto.request.ReminderSaveRequest;
import pvt.mktech.petcare.pet.entity.Pet;
import pvt.mktech.petcare.reminder.entity.Reminder;
import pvt.mktech.petcare.pet.service.PetService;
import pvt.mktech.petcare.reminder.service.ReminderService;

/**
 * 内部服务间调用 API
 * 用于替代 Dubbo 服务，支持 serverless 部署
 */
@Slf4j
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "internal.api.enabled", havingValue = "true", matchIfMissing = true)
public class InternalApiController {

    private final ReminderService reminderService;
    private final PetService petService;

    /**
     * AI 服务调用：保存提醒事项
     * 原 Dubbo 接口替代
     */
    @PostMapping("/reminder")
    public Boolean saveReminder(@RequestBody ReminderSaveRequest request) {
        log.info("内部API调用: saveReminder, request: {}", request);

        Reminder reminder = new Reminder();
        BeanUtil.copyProperties(request, reminder);

        // 查询宠物名称转换为 petId
        if (request.getPetName() != null && reminder.getUserId() != null) {
            Pet pet = petService.findByUserIdAndPetName(reminder.getUserId(), request.getPetName());
            if (pet != null) {
                reminder.setPetId(pet.getId());
            } else {
                log.warn("未找到宠物，userId: {}, petName: {}", reminder.getUserId(), request.getPetName());
            }
        }

        if (reminder.getNextTriggerTime() == null) {
            reminder.setNextTriggerTime(reminder.getScheduleTime());
        }

        return reminderService.save(reminder);
    }
}
