package pvt.mktech.petcare.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * 宠物健康记录 实体类。
 *
 * @author PetCare Code Generator
 * @since 2025-12-17
 */
@Data
@Table("tb_health_record")
public class HealthRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 宠物ID，关联pet表
     */
    private Long petId;

    /**
     * 记录类型
     */
    private String recordType;

    /**
     * 记录标题/提醒事项
     */
    private String title;

    /**
     * 详细描述/内容
     */
    private String description;

    /**
     * 记录时间/执行时间
     */
    private LocalDateTime recordTime;

    /**
     * 计划时间（用于提醒）
     */
    private LocalDateTime scheduleTime;

    /**
     * 提前提醒时间(分钟)
     */
    private Integer remindBeforeMinutes;

    /**
     * 重复类型
     */
    private String repeatType;

    /**
     * 重复配置，如每周几、每月几号等
     */
    private String repeatConfig;

    /**
     * 数值记录（体重/kg，体温/°C）
     */
    private BigDecimal value;

    /**
     * 药品名称
     */
    private String medicationInfo;

    /**
     * 是否完成
     */
    private Boolean isCompleted;

    /**
     * 完成时间
     */
    private LocalDateTime completedTime;

    private LocalDateTime createdAt;
}