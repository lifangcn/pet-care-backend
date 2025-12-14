package pvt.mktech.petcare.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * 宠物健康记录表 实体类。
 *
 * @author PetCare Code Generator
 * @since 2025-12-08
 */
@Data
@Table("tb_pet_health_records")
public class PetHealthRecords implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 宠物ID
     */
    private Long petId;

    /**
     * 记录类型: 1-体重 2-体温 3-症状 4-用药 5-其他
     */
    private Boolean recordType;

    /**
     * 记录数值
     */
    private BigDecimal value;

    /**
     * 单位
     */
    private String unit;

    /**
     * 备注
     */
    private String notes;

    /**
     * 记录时间
     */
    private LocalDateTime recordDate;

    /**
     * 创建时间
     */
    @Column(value = "created_at", onInsertValue = "CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

}
