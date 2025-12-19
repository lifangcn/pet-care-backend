package pvt.mktech.petcare.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;


/**
 * 宠物表 表定义层。
 *
 * @author PetCare Code Generator
 * @since 2025-12-02
 */
public class PetTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 宠物表
     */
    public static final PetTableDef PETS = new PetTableDef();

    /**
     * 宠物ID
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 宠物名称
     */
    public final QueryColumn NAME = new QueryColumn(this, "name");

    /**
     * 宠物类型
     */
    public final QueryColumn TYPE = new QueryColumn(this, "type");

    /**
     * 品种
     */
    public final QueryColumn BREED = new QueryColumn(this, "breed");

    /**
     * 性别: 0-未知 1-雄性 2-雌性
     */
    public final QueryColumn GENDER = new QueryColumn(this, "gender");

    /**
     * 用户ID
     */
    public final QueryColumn USER_ID = new QueryColumn(this, "user_id");

    /**
     * 体重(kg)
     */
    public final QueryColumn WEIGHT = new QueryColumn(this, "weight");

    /**
     * 生日
     */
    public final QueryColumn BIRTHDAY = new QueryColumn(this, "birthday");

    /**
     * 头像URL
     */
    public final QueryColumn AVATAR = new QueryColumn(this, "avatar");

    /**
     * 创建时间
     */
    public final QueryColumn CREATED_AT = new QueryColumn(this, "created_at");

    /**
     * 健康备注
     */
    public final QueryColumn HEALTH_NOTES = new QueryColumn(this, "health_notes");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, USER_ID, NAME, TYPE, BREED, GENDER, BIRTHDAY, WEIGHT, AVATAR, HEALTH_NOTES, CREATED_AT};

    public PetTableDef() {
        super("", "tb_pet");
    }

    private PetTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public PetTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new PetTableDef("", "tb_pet", alias));
    }

}
