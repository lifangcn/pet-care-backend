package pvt.mktech.petcare.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;


/**
 * 宠物疫苗记录表 表定义层。
 *
 * @author PetCare Code Generator
 * @since 2025-12-08
 */
public class PetVaccinationsTableDef extends TableDef {

    private static final long serialVersionUID = 1L;

    /**
     * 宠物疫苗记录表
     */
    public static final PetVaccinationsTableDef PET_VACCINATIONS = new PetVaccinationsTableDef();

    /**
     * 记录ID
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 备注
     */
    public final QueryColumn NOTES = new QueryColumn(this, "notes");

    /**
     * 宠物ID
     */
    public final QueryColumn PET_ID = new QueryColumn(this, "pet_id");

    /**
     * 创建时间
     */
    public final QueryColumn CREATED_AT = new QueryColumn(this, "created_at");

    /**
     * 更新时间
     */
    public final QueryColumn UPDATED_AT = new QueryColumn(this, "updated_at");

    /**
     * 下次接种日期
     */
    public final QueryColumn NEXT_DUE_DATE = new QueryColumn(this, "next_due_date");

    /**
     * 疫苗名称
     */
    public final QueryColumn VACCINE_NAME = new QueryColumn(this, "vaccine_name");

    /**
     * 附件URL
     */
    public final QueryColumn ATTACHMENT_URL = new QueryColumn(this, "attachment_url");

    /**
     * 接种日期
     */
    public final QueryColumn VACCINATION_DATE = new QueryColumn(this, "vaccination_date");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, PET_ID, VACCINE_NAME, VACCINATION_DATE, NEXT_DUE_DATE, NOTES, ATTACHMENT_URL, CREATED_AT, UPDATED_AT};

    public PetVaccinationsTableDef() {
        super("", "tb_pet_vaccinations");
    }

    private PetVaccinationsTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public PetVaccinationsTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new PetVaccinationsTableDef("", "tb_pet_vaccinations", alias));
    }

}
