package pvt.mktech.petcare.core.service.dubbo;

import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.api.dto.ReminderSaveRequest;
import pvt.mktech.petcare.api.service.ReminderDubboService;
import pvt.mktech.petcare.common.usercache.UserContext;
import pvt.mktech.petcare.core.entity.Pet;
import pvt.mktech.petcare.core.entity.Reminder;
import pvt.mktech.petcare.core.service.PetService;
import pvt.mktech.petcare.core.service.ReminderService;

@Slf4j
@Service
@DubboService(interfaceClass = ReminderDubboService.class, version = "1.0.0")
@RequiredArgsConstructor
public class ReminderDubboServiceImpl implements ReminderDubboService {

    private final ReminderService reminderService;
    private final PetService petService;

    @Override
    public boolean saveReminder(ReminderSaveRequest request) {
        Reminder reminder = new Reminder();
        BeanUtil.copyProperties(request, reminder);

        // 查询宠物名称
        if (reminder.getUserId() == null && UserContext.getUserInfo() != null) {
            reminder.setUserId(UserContext.getUserInfo().getUserId());
        }

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

