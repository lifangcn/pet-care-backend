package pvt.mktech.petcare.points.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pvt.mktech.petcare.points.event.CouponRedeemEvent;
import pvt.mktech.petcare.points.service.PointsService;

/**
 * {@code @description}: 券兑换积分事件监听器
 * {@code @date}: 2026/02/14
 *
 * @author Michael
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponRedeemListener {

    private final PointsService pointsService;

    /**
     * 处理券兑换积分事件
     * 事务提交前执行，确保积分变更与主业务在同一事务内
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleCouponRedeem(CouponRedeemEvent event) {
        try {
            pointsService.earnByCoupon(event.getUserId(), event.getCouponId());
            log.info("券兑换积分成功: userId={}, couponId={}", event.getUserId(), event.getCouponId());
        } catch (Exception e) {
            log.error("券兑换积分失败: userId={}, couponId={}, error={}",
                    event.getUserId(), event.getCouponId(), e.getMessage(), e);
            throw e; // 事务内抛出异常，触发回滚
        }
    }
}
