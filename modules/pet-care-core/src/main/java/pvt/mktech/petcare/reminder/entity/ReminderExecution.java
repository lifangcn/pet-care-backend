package pvt.mktech.petcare.reminder.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 提醒执行记录表 实体类。
 *
 * @author PetCare Code Generator
 * @since 2025-12-22
 */
@Table("tb_reminder_execution")
@Data
public class ReminderExecution implements Serializable {

    @Serial
    private static final long serialVersionUID = 7142870712957892246L;
    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 提醒ID
     */
    private Long reminderId;

    /**
     * 宠物ID
     */
    private Long petId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 计划执行时间
     */
    private LocalDateTime scheduleTime;

    /**
     * 实际执行时间
     */
    private LocalDateTime actualTime;

    /**
     * 执行状态
     */
    private String status;

    /**
     * 完成说明
     */
    private String completionNotes;

    /**
     * 通知时间
     */
    private LocalDateTime notificationTime;

    /**
     * 是否已读
     */
    private Boolean isRead;

    /**
     * 是否已发送
     */
    private Boolean isSent;

    /**
     * 发送时间
     */
    private LocalDateTime sentAt;

    /**
     * 阅读时间
     */
    private LocalDateTime readAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

}
