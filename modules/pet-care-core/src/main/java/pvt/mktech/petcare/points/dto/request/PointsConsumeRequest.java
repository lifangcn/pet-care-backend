package pvt.mktech.petcare.points.dto.request;

import lombok.Data;

import java.io.Serializable;

/**
 * {@code @description}: 积分消耗请求
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
@Data
public class PointsConsumeRequest implements Serializable {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 行为类型
     */
    private Integer actionType;

    /**
     * 消耗积分数
     */
    private Integer points;

    /**
     * 关联业务类型
     */
    private String bizType;

    /**
     * 关联业务ID
     */
    private Long bizId;

    /**
     * 使用的代金券ID
     */
    private Long couponId;
    /**
     * 聊天ID
     */
    private String conversationId;
}
