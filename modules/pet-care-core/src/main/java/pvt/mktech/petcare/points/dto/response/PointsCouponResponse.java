package pvt.mktech.petcare.points.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * {@code @description}: 积分代金券响应
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
@Data
public class PointsCouponResponse implements Serializable {

    /**
     * 券ID
     */
    private Long id;

    /**
     * 面值
     */
    private Integer faceValue;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 失效时间
     */
    private LocalDateTime endTime;
}
