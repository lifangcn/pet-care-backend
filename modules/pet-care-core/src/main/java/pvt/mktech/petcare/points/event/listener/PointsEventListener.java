package pvt.mktech.petcare.points.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pvt.mktech.petcare.points.dto.request.PointsConsumeRequest;
import pvt.mktech.petcare.points.entity.codelist.PointsActionType;
import pvt.mktech.petcare.points.event.PointsConsumeEvent;
import pvt.mktech.petcare.points.event.PointsEarnEvent;
import pvt.mktech.petcare.points.service.PointsService;

/**
 * {@code @description}: 积分事件监听器
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointsEventListener {

    private final PointsService pointsService;

    /**
     * 处理积分获取事件
     * 只有主业务事务提交后才会执行
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePointsEarnEvent(PointsEarnEvent event) {
        try {
            PointsActionType action = event.getActionType();

            // 质量类积分（被点赞、被评论）
            if (action == PointsActionType.LIKED || action == PointsActionType.COMMENTED) {
                pointsService.earnByQuality(
                        event.getUserId(),
                        action,
                        event.getTargetId(),
                        event.getInteractUserId()
                );
            } else {
                // 主动行为积分（签到、发布、评论、点赞）
                pointsService.earnByAction(
                        event.getUserId(),
                        action,
                        event.getBizId()
                );
            }

            log.info("积分获取成功: userId={}, action={}, points={}",
                    event.getUserId(), action, action.getPoints());
        } catch (Exception e) {
            log.error("积分获取失败: userId={}, action={}, error={}",
                    event.getUserId(), event.getActionType(), e.getMessage(), e);
            // TODO: 记录失败事件，后期补偿
        }
    }

    /**
     * 处理积分消耗事件
     * 只有主业务事务提交后才会执行
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePointsConsumeEvent(PointsConsumeEvent event) {
        try {
            PointsConsumeRequest request = new PointsConsumeRequest();
            request.setUserId(event.getUserId());
            request.setActionType(event.getActionType().getCode());
            request.setPoints(event.getPoints());
            request.setBizId(event.getBizId());
            request.setBizType(event.getBizType());
            request.setCouponId(event.getCouponId());

            pointsService.consume(request);

            log.info("积分消耗成功: userId={}, points={}, couponId={}",
                    event.getUserId(), event.getPoints(), event.getCouponId());
        } catch (Exception e) {
            log.error("积分消耗失败: userId={}, points={}, error={}",
                    event.getUserId(), event.getPoints(), e.getMessage(), e);
            // TODO: 记录失败事件，后期补偿
        }
    }
}
