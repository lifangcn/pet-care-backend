package pvt.mktech.petcare.points.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * {@code @description}: 积分等级响应
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
@Data
public class PointsLevelResponse implements Serializable {

    /**
     * 等级
     */
    private Integer level;

    /**
     * 等级称号
     */
    private String title;

    /**
     * 所需积分
     */
    private Integer requiredPoints;

    /**
     * 下一等级所需积分
     */
    private Integer nextLevelPoints;

    /**
     * 等级权益
     */
    private List<String> benefits;
}
