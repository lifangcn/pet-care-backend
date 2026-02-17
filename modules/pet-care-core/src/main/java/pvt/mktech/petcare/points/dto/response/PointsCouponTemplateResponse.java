package pvt.mktech.petcare.points.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * {@code @description}: 积分券模板响应
 * {@code @date}: 2026/02/16
 *
 * @author Michael Li
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsCouponTemplateResponse implements Serializable {

    /**
     * 模板ID
     */
    private Long id;

    /**
     * 券名称
     */
    private String name;

    /**
     * 面值（可抵扣积分数）
     */
    private Integer faceValue;

    /**
     * 剩余库存
     */
    private Long stock;

    /**
     * 每人限领数量
     */
    private Integer perUserLimit;

    /**
     * 有效期描述
     */
    private String validDesc;
}
