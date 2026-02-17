package pvt.mktech.petcare.points.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import pvt.mktech.petcare.points.entity.codelist.SourceTypeOfCouponTemplate;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * {@code @description}: 积分代金券模板实体类
 * 对应数据库表 tb_points_coupon_template
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
@Data
@Table("tb_points_coupon_template")
public class PointsCouponTemplate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
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
     * 有效天数（领取后计算）
     */
    private Integer validDays;

    /**
     * 发放总量（0表示不限）
     */
    private Integer totalCount;

    /**
     * 已发放数量
     */
    private Integer issuedCount;

    /**
     * 每人限领数量
     */
    private Integer perUserLimit;

    /**
     * 来源类型：SYSTEM-系统发放 ACTIVITY-活动发放 NEWCOMER-新人礼包
     */
    private SourceTypeOfCouponTemplate sourceType;

    /**
     * 状态：0-停用 1-启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    @Column(value = "created_at", onInsertValue = "CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(value = "updated_at", onInsertValue = "CURRENT_TIMESTAMP", onUpdateValue = "CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

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
