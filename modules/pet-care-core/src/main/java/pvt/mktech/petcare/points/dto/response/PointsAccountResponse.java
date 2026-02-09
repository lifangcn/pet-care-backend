package pvt.mktech.petcare.points.dto.response;

import lombok.Data;

import java.io.Serializable;

/**
 * {@code @description}: 积分账户响应
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
@Data
public class PointsAccountResponse implements Serializable {

    /**
     * 可用积分
     */
    private Integer availablePoints;

    /**
     * 累计积分
     */
    private Integer totalPoints;

    /**
     * 用户等级
     */
    private Integer level;
}
