package pvt.mktech.petcare.reminder.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import pvt.mktech.petcare.reminder.entity.codelist.RepeatTypeOfReminder;
import pvt.mktech.petcare.reminder.entity.codelist.SourceTypeOfReminder;

import java.io.Serial;
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

    @Serial
    private static final long serialVersionUID = 3562793547239601084L;
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
     * 记录来源：MANUAL, HEALTH_RECORD, SYSTEM
     */
    private SourceTypeOfReminder sourceType;

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

    /**
     * 记录时间
     */
    private LocalDateTime recordTime;

    /**
     * 计划时间
     */
    private LocalDateTime scheduleTime;

    /**
     * 计划时间
     */
    private LocalDateTime nextTriggerTime;

    /**
     * 提前提醒时间(分钟)
     */
    private Integer remindBeforeMinutes;

    /**
     * 重复类型: NONE(不重复), DAILY(每天), WEEKLY(每周), MONTHLY(每月), CUSTOM(自定义)
     */
    private RepeatTypeOfReminder repeatType;

    /**
     * 重复配置(自定义重复规则)
     */
    private String repeatConfig;

    /**
     * 是否激活
     */
    private Boolean isActive;

    /**
     * 提醒执行记录ID，当前提醒和执行记录关联，标识最新的执行情况
     */
    private Long reminderExecutionId;

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

    /**
     * 逻辑删除：0-正常，1-已删除
     */
    private Boolean isDeleted;

    /**
     * 删除时间
     */
    private LocalDateTime deletedAt;

}
