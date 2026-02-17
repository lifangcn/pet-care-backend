package pvt.mktech.petcare.points.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import pvt.mktech.petcare.points.entity.codelist.ActionTypeOfPointsRecord;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * {@code @description}: 积分流水记录实体类
 * 对应数据库表 tb_points_record
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
@Data
@Table("tb_points_record")
public class PointsRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 积分变动值（正为获取，负为消耗）
     */
    private Integer points;

    /**
     * 变动前可用积分
     */
    private Integer pointsBefore;

    /**
     * 变动后可用积分
     */
    private Integer pointsAfter;

    /**
     * 行为类型（使用枚举）
     */
    @Column(isLarge = true)
    private ActionTypeOfPointsRecord actionType;

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
     * 代金券抵扣积分数
     */
    private Integer couponDeduct;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 创建时间
     */
    @Column(value = "created_at", onInsertValue = "CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}
