package pvt.mktech.petcare.points.event;

import lombok.Getter;
import lombok.Setter;
import pvt.mktech.petcare.points.entity.codelist.ActionTypeOfPointsRecord;

import java.time.LocalDateTime;

/**
 * {@code @description}: 积分获取事件
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
@Getter
@Setter
public class PointsEarnEvent {

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
     * 目标ID（质量类积分使用，如内容ID）
     */
    private Long targetId;

    /**
     * 互动用户ID（质量类积分使用，如点赞/评论的用户ID）
     */
    private Long interactUserId;

    /**
     * 事件时间
     */
    private LocalDateTime timestamp;

    public PointsEarnEvent() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 积分获取事件构造函数
     *
     * @param userId     用户ID，用于标识获取积分的用户
     * @param actionType 积分操作类型，定义了获取积分的具体行为
     * @param bizId      业务ID，关联具体的业务场景或订单等
     */
    public PointsEarnEvent(Long userId, ActionTypeOfPointsRecord actionType, Long bizId) {
        this.userId = userId;
        this.actionType = actionType;
        this.bizId = bizId;
        this.timestamp = LocalDateTime.now();
    }

    public PointsEarnEvent(Long userId, ActionTypeOfPointsRecord actionType, Long bizId, Long targetId, Long interactUserId) {
        this.userId = userId;
        this.actionType = actionType;
        this.bizId = bizId;
        this.targetId = targetId;
        this.interactUserId = interactUserId;
        this.timestamp = LocalDateTime.now();
    }
}
