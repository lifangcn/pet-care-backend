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
public class PetsTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 宠物表
     */
    public static final PetsTableDef PETS = new PetsTableDef();

    /**
     * 宠物ID
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 宠物名称
     */
    public final QueryColumn NAME = new QueryColumn(this, "name");

    /**
     * 宠物类型: 1-狗 2-猫 3-其他
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
     * 状态: 0-删除 1-正常
     */
    public final QueryColumn STATUS = new QueryColumn(this, "status");

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
    public final QueryColumn AVATAR_URL = new QueryColumn(this, "avatar_url");

    /**
     * 创建时间
     */
    public final QueryColumn CREATED_AT = new QueryColumn(this, "created_at");

    /**
     * 更新时间
     */
    public final QueryColumn UPDATED_AT = new QueryColumn(this, "updated_at");

    /**
     * 过敏信息
     */
    public final QueryColumn ALLERGY_INFO = new QueryColumn(this, "allergy_info");

    /**
     * 健康备注
     */
    public final QueryColumn HEALTH_NOTES = new QueryColumn(this, "health_notes");

    /**
     * 是否绝育: 0-否 1-是
     */
    public final QueryColumn IS_STERILIZED = new QueryColumn(this, "is_sterilized");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, USER_ID, NAME, TYPE, BREED, GENDER, BIRTHDAY, WEIGHT, AVATAR_URL, IS_STERILIZED, HEALTH_NOTES, ALLERGY_INFO, STATUS, CREATED_AT, UPDATED_AT};

    public PetsTableDef() {
        super("", "tb_pets");
    }

    private PetsTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public PetsTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new PetsTableDef("", "tb_pets", alias));
    }

}
