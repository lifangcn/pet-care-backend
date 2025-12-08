package pvt.mktech.petcare.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * 宠物健康记录表 实体类。
 *
 * @author PetCare Code Generator
 * @since 2025-12-08
 */
@Table("tb_pet_health_records")
public class PetHealthRecords implements Serializable {

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
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPetId() {
        return petId;
    }

    public void setPetId(Long petId) {
        this.petId = petId;
    }

    public Boolean getRecordType() {
        return recordType;
    }

    public void setRecordType(Boolean recordType) {
        this.recordType = recordType;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDateTime recordDate) {
        this.recordDate = recordDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
