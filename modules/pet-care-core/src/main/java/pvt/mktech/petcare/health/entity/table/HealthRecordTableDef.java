package pvt.mktech.petcare.health.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;


/**
 * 健康记录表 表定义层。
 *
 * @author PetCare Code Generator
 * @since 2025-12-22
 */
public class HealthRecordTableDef extends TableDef {
    /**
     * 健康记录表
     */
    public static final HealthRecordTableDef HEALTH_RECORD = new HealthRecordTableDef();

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
     * 数值(体重/体温等)
     */
    public final QueryColumn VALUE = new QueryColumn(this, "value");

    /**
     * 用户ID
     */
    public final QueryColumn USER_ID = new QueryColumn(this, "user_id");

    /**
     * 症状信息
     */
    public final QueryColumn SYMPTOM = new QueryColumn(this, "symptom");

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
     * 记录类型: WEIGHT(体重), TEMPERATURE(体温), MEDICAL(用药)
     */
    public final QueryColumn RECORD_TYPE = new QueryColumn(this, "record_type");

    /**
     * 描述
     */
    public final QueryColumn DESCRIPTION = new QueryColumn(this, "description");

    /**
     * 用药信息
     */
    public final QueryColumn MEDICATION_INFO = new QueryColumn(this, "medication_info");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, PET_ID, USER_ID, RECORD_TYPE, TITLE, DESCRIPTION, RECORD_TIME, VALUE, SYMPTOM, MEDICATION_INFO, CREATED_AT, UPDATED_AT};

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
