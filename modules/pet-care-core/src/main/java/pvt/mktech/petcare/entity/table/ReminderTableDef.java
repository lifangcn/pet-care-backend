package pvt.mktech.petcare.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;


/**
 * 提醒事件表 表定义层。
 *
 * @author PetCare Code Generator
 * @since 2025-12-22
 */
public class ReminderTableDef extends TableDef {

    private static final long serialVersionUID = 1L;

    /**
     * 提醒事件表
     */
    public static final ReminderTableDef REMINDER = new ReminderTableDef();

    /**
     * 主键ID
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 宠物ID
     */
    public final QueryColumn PET_ID = new QueryColumn(this, "pet_id");

    /**
     * 标题
     */
    public final QueryColumn TITLE = new QueryColumn(this, "title");

    /**
     * 用户ID
     */
    public final QueryColumn USER_ID = new QueryColumn(this, "user_id");

    /**
     * 是否激活
     */
    public final QueryColumn IS_ACTIVE = new QueryColumn(this, "is_active");

    /**
     * 来源ID（如健康记录ID）
     */
    public final QueryColumn SOURCE_ID = new QueryColumn(this, "source_id");

    /**
     * 创建时间
     */
    public final QueryColumn CREATED_AT = new QueryColumn(this, "created_at");

    /**
     * 更新时间
     */
    public final QueryColumn UPDATED_AT = new QueryColumn(this, "updated_at");

    /**
     * 记录时间
     */
    public final QueryColumn RECORD_TIME = new QueryColumn(this, "record_time");

    /**
     * 重复类型: none(不重复), daily(每天), weekly(每周), monthly(每月), custom(自定义)
     */
    public final QueryColumn REPEAT_TYPE = new QueryColumn(this, "repeat_type");

    /**
     * 记录来源：manual, health_record, system
     */
    public final QueryColumn SOURCE_TYPE = new QueryColumn(this, "source_type");

    /**
     * 描述
     */
    public final QueryColumn DESCRIPTION = new QueryColumn(this, "description");

    /**
     * 重复配置(自定义重复规则)
     */
    public final QueryColumn REPEAT_CONFIG = new QueryColumn(this, "repeat_config");

    /**
     * 计划时间(用于提醒)
     */
    public final QueryColumn SCHEDULE_TIME = new QueryColumn(this, "schedule_time");

    /**
     * 完成时间
     */
    public final QueryColumn COMPLETED_TIME = new QueryColumn(this, "completed_time");

    /**
     * 已完成次数
     */
    public final QueryColumn COMPLETED_COUNT = new QueryColumn(this, "completed_count");

    /**
     * 总执行次数
     */
    public final QueryColumn TOTAL_OCCURRENCES = new QueryColumn(this, "total_occurrences");

    /**
     * 提前提醒时间(分钟)
     */
    public final QueryColumn REMIND_BEFORE_MINUTES = new QueryColumn(this, "remind_before_minutes");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, PET_ID, USER_ID, SOURCE_TYPE, SOURCE_ID, TITLE, DESCRIPTION, RECORD_TIME, SCHEDULE_TIME, REMIND_BEFORE_MINUTES, REPEAT_TYPE, REPEAT_CONFIG, IS_ACTIVE, TOTAL_OCCURRENCES, COMPLETED_COUNT, COMPLETED_TIME, CREATED_AT, UPDATED_AT};

    public ReminderTableDef() {
        super("", "tb_reminder");
    }

    private ReminderTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public ReminderTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new ReminderTableDef("", "tb_reminder", alias));
    }

}
