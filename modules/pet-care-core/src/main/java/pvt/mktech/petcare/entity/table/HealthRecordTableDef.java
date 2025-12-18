package pvt.mktech.petcare.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;


/**
 * 宠物健康记录表 表定义层。
 *
 * @author PetCare Code Generator
 * @since 2025-12-17
 */
public class HealthRecordTableDef extends TableDef {

    private static final long serialVersionUID = 1L;

    /**
     * 宠物健康记录
     */
    public static final HealthRecordTableDef HEALTH_RECORD = new HealthRecordTableDef();


    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 宠物ID，关联pet表
     */
    public final QueryColumn PET_ID = new QueryColumn(this, "pet_id");

    /**
     * 记录标题/提醒事项
     */
    public final QueryColumn TITLE = new QueryColumn(this, "title");

    /**
     * 数值记录（体重/kg，体温/°C）
     */
    public final QueryColumn VALUE = new QueryColumn(this, "value");


    public final QueryColumn CREATED_AT = new QueryColumn(this, "created_at");

    /**
     * 记录时间/执行时间
     */
    public final QueryColumn RECORD_TIME = new QueryColumn(this, "record_time");

    /**
     * 记录类型
     */
    public final QueryColumn RECORD_TYPE = new QueryColumn(this, "record_type");

    /**
     * 重复类型
     */
    public final QueryColumn REPEAT_TYPE = new QueryColumn(this, "repeat_type");

    /**
     * 详细描述/内容
     */
    public final QueryColumn DESCRIPTION = new QueryColumn(this, "description");

    /**
     * 是否完成
     */
    public final QueryColumn IS_COMPLETED = new QueryColumn(this, "is_completed");

    /**
     * 重复配置，如每周几、每月几号等
     */
    public final QueryColumn REPEAT_CONFIG = new QueryColumn(this, "repeat_config");

    /**
     * 计划时间（用于提醒）
     */
    public final QueryColumn SCHEDULE_TIME = new QueryColumn(this, "schedule_time");

    /**
     * 完成时间
     */
    public final QueryColumn COMPLETED_TIME = new QueryColumn(this, "completed_time");

    /**
     * 药品名称
     */
    public final QueryColumn MEDICATION_INFO = new QueryColumn(this, "medication_info");

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
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, PET_ID, RECORD_TYPE, TITLE, DESCRIPTION, RECORD_TIME, SCHEDULE_TIME, REMIND_BEFORE_MINUTES, REPEAT_TYPE, REPEAT_CONFIG, VALUE, MEDICATION_INFO, IS_COMPLETED, COMPLETED_TIME, CREATED_AT};

    public HealthRecordTableDef() {
        super("", "tb_health_record");
    }

    private HealthRecordTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public HealthRecordTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new HealthRecordTableDef("", "tb_health_record", alias));
    }
}