package pvt.mktech.petcare.points.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import pvt.mktech.petcare.points.entity.codelist.StatusOfPointsCoupon;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * {@code @description}: 用户积分代金券实体类
 * 对应数据库表 tb_points_coupon
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
@Data
@Table("tb_points_coupon")
public class PointsCoupon implements Serializable {

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
     * 券模板ID
     */
    private Long templateId;

    /**
     * 面值（冗余，防止模板变更）
     */
    private Integer faceValue;

    /**
     * 状态：0-未使用 1-已使用 2-已过期
     */
    private StatusOfPointsCoupon status;

    /**
     * 生效时间
     */
    private LocalDateTime startTime;

    /**
     * 失效时间
     */
    private LocalDateTime endTime;

    /**
     * 使用时间
     */
    private LocalDateTime usedTime;

    /**
     * 使用时关联的流水ID
     */
    private Long usedRecordId;

    /**
     * 创建时间
     */
    @Column(value = "created_at", onInsertValue = "CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    /**
     * 逻辑删除：0-正常，1-已删除
     */
    @Column(value = "is_deleted", onInsertValue = "0")
    private Boolean isDeleted;

    /**
     * 删除时间
     */
    @Column(value = "deleted_at")
    private LocalDateTime deletedAt;
}
