package pvt.mktech.petcare.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;


/**
 * 宠物健康记录表 表定义层。
 *
 * @author PetCare Code Generator
 * @since 2025-12-08
 */
public class HealthRecordTableDef extends TableDef {

    private static final long serialVersionUID = 1L;

    /**
     * 宠物健康记录表
     */
    public static final HealthRecordTableDef HEALTH_RECORD = new HealthRecordTableDef();

    /**
     * 记录ID
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 单位
     */
    public final QueryColumn UNIT = new QueryColumn(this, "unit");

    /**
     * 备注
     */
    public final QueryColumn NOTES = new QueryColumn(this, "notes");

    /**
     * 宠物ID
     */
    public final QueryColumn PET_ID = new QueryColumn(this, "pet_id");

    /**
     * 记录数值
     */
    public final QueryColumn VALUE = new QueryColumn(this, "value");

    /**
     * 创建时间
     */
    public final QueryColumn CREATED_AT = new QueryColumn(this, "created_at");

    /**
     * 记录时间
     */
    public final QueryColumn RECORD_DATE = new QueryColumn(this, "record_date");

    /**
     * 记录类型: 1-体重 2-体温 3-症状 4-用药 5-其他
     */
    public final QueryColumn RECORD_TYPE = new QueryColumn(this, "record_type");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, PET_ID, RECORD_TYPE, VALUE, UNIT, NOTES, RECORD_DATE, CREATED_AT};

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
