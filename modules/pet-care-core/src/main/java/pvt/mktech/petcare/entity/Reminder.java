package pvt.mktech.petcare.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 提醒事件表 实体类。
 *
 * @author PetCare Code Generator
 * @since 2025-12-22
 */
@Table("tb_reminder")
@Data
public class Reminder implements Serializable {

    private static final long serialVersionUID = 1L;

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
     * 记录来源：manual, health_record, system
     */
    private String sourceType;

    /**
     * 来源ID（如健康记录ID）
     */
    private Long sourceId;

    /**
     * 标题
     */
    private String title;

    /**
     * 描述
     */
    private String description;

    /* 时间提醒需要满足的场景：
    1.绝对时间：repeatType=none and scheduleTime
    2.相对时间：根据设置时间，前端计算出具体时间
    3.周期性: repeatType=daily and scheduleTime，之后根据scheduleTime计算出具体时间
    *
    *
    * */

    /**
     * 记录时间
     */
    private LocalDateTime recordTime;

    /**
     * 计划时间
     */
    private LocalDateTime scheduleTime;

    /**
     * 提前提醒时间(分钟)
     */
    private Integer remindBeforeMinutes;

    /**
     * 重复类型: none(不重复), daily(每天), weekly(每周), monthly(每月), custom(自定义)
     */
    private String repeatType;

    /**
     * 重复配置(自定义重复规则)
     */
    private String repeatConfig;

    /**
     * 是否激活
     */
    private Boolean isActive;

    /**
     * 总执行次数
     */
    private Integer totalOccurrences;

    /**
     * 已完成次数
     */
    private Integer completedCount;

    /**
     * 完成时间
     */
    private LocalDateTime completedTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

}
