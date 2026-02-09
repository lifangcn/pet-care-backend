package pvt.mktech.petcare.shared.controller;

import cn.hutool.core.bean.BeanUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import pvt.mktech.petcare.reminder.dto.request.ReminderSaveRequest;
import pvt.mktech.petcare.pet.entity.Pet;
import pvt.mktech.petcare.reminder.entity.Reminder;
import pvt.mktech.petcare.pet.service.PetService;
import pvt.mktech.petcare.reminder.service.ReminderService;
import pvt.mktech.petcare.points.dto.request.PointsConsumeRequest;
import pvt.mktech.petcare.points.service.PointsService;
import pvt.mktech.petcare.points.entity.codelist.PointsActionType;

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
    private final PointsService pointsService;

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

        boolean saved = reminderService.save(reminder);
        log.info("提醒事项保存结果: {}", saved);
        return saved;
    }

    /**
     * AI 积分扣除请求
     */
    public record AiPointsConsumeRequest(Long userId, String conversationId) {}

    /**
     * AI 服务调用：扣除咨询积分
     * AI 咨询成功后调用此接口扣除用户积分
     */
    @PostMapping("/points/consume-ai")
    public Boolean consumeAiPoints(@RequestBody AiPointsConsumeRequest request) {
        log.info("内部API调用: consumeAiPoints, userId: {}", request.userId());

        PointsConsumeRequest consumeRequest = new PointsConsumeRequest();
        consumeRequest.setUserId(request.userId());
        consumeRequest.setActionType(PointsActionType.AI_CONSULT.getCode());
        consumeRequest.setPoints(PointsActionType.AI_CONSULT.getPoints());
        consumeRequest.setBizType("AI_CONSULT");

        try {
            boolean result = pointsService.consume(consumeRequest);
            if (result) {
                log.info("AI咨询积分扣除成功, userId: {}, points: {}", request.userId(), PointsActionType.AI_CONSULT.getPoints());
            } else {
                log.warn("AI咨询积分扣除失败, userId: {}, points: {}", request.userId(), PointsActionType.AI_CONSULT.getPoints());
            }
            return result;
        } catch (Exception e) {
            log.error("AI咨询积分扣除失败, userId: {}", request.userId(), e);
            return false;
        }
    }
}
