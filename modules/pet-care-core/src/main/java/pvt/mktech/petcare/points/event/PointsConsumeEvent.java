package pvt.mktech.petcare.points.event;

import lombok.Data;
import pvt.mktech.petcare.points.entity.codelist.ActionTypeOfPointsRecord;

/**
 * {@code @description}: 积分消耗事件
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
@Data
public class PointsConsumeEvent {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 行为类型
     */
    private ActionTypeOfPointsRecord actionType;

    /**
     * 关联业务ID
     */
    private Long bizId;

    /**
     * 消耗积分数（负值）
     */
    private Integer points;

    /**
     * 使用的代金券ID
     */
    private Long couponId;

    /**
     * 关联业务类型
     */
    private String bizType;

    /**
     * 事件时间
     */
    private java.time.LocalDateTime timestamp;

    public PointsConsumeEvent() {
        this.timestamp = java.time.LocalDateTime.now();
    }

    public PointsConsumeEvent(Long userId, ActionTypeOfPointsRecord actionType, Integer points, Long bizId, Long couponId, String bizType) {
        this.userId = userId;
        this.actionType = actionType;
        this.points = points;
        this.bizId = bizId;
        this.couponId = couponId;
        this.bizType = bizType;
        this.timestamp = java.time.LocalDateTime.now();
    }
}
