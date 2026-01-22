package pvt.mktech.petcare.health.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import pvt.mktech.petcare.health.entity.codelist.RecordTypeOfHealthRecord;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * 健康记录表 实体类。
 *
 * @author PetCare Code Generator
 * @since 2025-12-22
 */
@Table("tb_health_record")
@Data
public class HealthRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 4221157116160362663L;
    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 宠物ID
     */
    private Long petId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 记录类型: weight(体重), temperature(体温), medical(用药)
     */
    private RecordTypeOfHealthRecord recordType;

    /**
     * 标题
     */
    private String title;

    /**
     * 描述
     */
    private String description;

    /**
     * 记录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordTime;

    /**
     * 数值(体重/体温等)
     */
    private BigDecimal value;

    /**
     * 症状信息
     */
    private String symptom;

    /**
     * 用药信息
     */
    private String medicationInfo;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonIgnore
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonIgnore
    private LocalDateTime updatedAt;

}
