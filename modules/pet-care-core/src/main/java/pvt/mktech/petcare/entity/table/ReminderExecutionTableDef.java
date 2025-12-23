package pvt.mktech.petcare.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;


/**
 * 提醒执行记录表 表定义层。
 *
 * @author PetCare Code Generator
 * @since 2025-12-22
 */
public class ReminderExecutionTableDef extends TableDef {

    private static final long serialVersionUID = 1L;

    /**
     * 提醒执行记录表
     */
    public static final ReminderExecutionTableDef REMINDER_EXECUTION = new ReminderExecutionTableDef();

    /**
     * 主键ID
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 宠物ID
     */
    public final QueryColumn PET_ID = new QueryColumn(this, "pet_id");

    /**
     * 是否已读
     */
    public final QueryColumn IS_READ = new QueryColumn(this, "is_read");

    /**
     * 是否已发送
     */
    public final QueryColumn IS_SENT = new QueryColumn(this, "is_sent");

    /**
     * 阅读时间
     */
    public final QueryColumn READ_AT = new QueryColumn(this, "read_at");

    /**
     * 发送时间
     */
    public final QueryColumn SENT_AT = new QueryColumn(this, "sent_at");

    /**
     * 执行状态
     */
    public final QueryColumn STATUS = new QueryColumn(this, "status");

    /**
     * 用户ID
     */
    public final QueryColumn USER_ID = new QueryColumn(this, "user_id");

    /**
     * 创建时间
     */
    public final QueryColumn CREATED_AT = new QueryColumn(this, "created_at");

    /**
     * 实际执行时间
     */
    public final QueryColumn ACTUAL_TIME = new QueryColumn(this, "actual_time");

    /**
     * 提醒ID
     */
    public final QueryColumn REMINDER_ID = new QueryColumn(this, "reminder_id");

    /**
     * 计划执行时间
     */
    public final QueryColumn SCHEDULED_TIME = new QueryColumn(this, "scheduled_time");

    /**
     * 完成说明
     */
    public final QueryColumn COMPLETION_NOTES = new QueryColumn(this, "completion_notes");

    /**
     * 通知时间
     */
    public final QueryColumn NOTIFICATION_TIME = new QueryColumn(this, "notification_time");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, REMINDER_ID, PET_ID, USER_ID, SCHEDULED_TIME, ACTUAL_TIME, STATUS, COMPLETION_NOTES, NOTIFICATION_TIME, IS_READ, IS_SENT, SENT_AT, READ_AT, CREATED_AT};

    public ReminderExecutionTableDef() {
        super("", "tb_reminder_execution");
    }

    private ReminderExecutionTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public ReminderExecutionTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new ReminderExecutionTableDef("", "tb_reminder_execution", alias));
    }

}
