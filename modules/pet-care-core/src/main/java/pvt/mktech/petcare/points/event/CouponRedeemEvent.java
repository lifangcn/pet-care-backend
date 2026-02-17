package pvt.mktech.petcare.points.event;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * {@code @description}: 券兑换积分事件
 * {@code @date}: 2026/02/14
 *
 * @author Michael
 */
@Data
public class CouponRedeemEvent {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 券ID
     */
    private Long couponId;

    /**
     * 事件时间
     */
    private LocalDateTime timestamp;

    public CouponRedeemEvent(Long userId, Long couponId) {
        this.userId = userId;
        this.couponId = couponId;
        this.timestamp = LocalDateTime.now();
    }
}
