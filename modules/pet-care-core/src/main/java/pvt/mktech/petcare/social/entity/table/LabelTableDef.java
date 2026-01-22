package pvt.mktech.petcare.social.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

public class LabelTableDef extends TableDef {

    /**
     * 标签表
     */
    public static final LabelTableDef LABEL = new LabelTableDef();

    /**
     * 主键ID
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 标签名
     */
    public final QueryColumn NAME = new QueryColumn(this, "name");

    /**
     * 1-通用标签 2-宠物品种 3-内容类型
     */
    public final QueryColumn TYPE = new QueryColumn(this, "type");

    /**
     * 标签图标
     */
    public final QueryColumn ICON = new QueryColumn(this, "icon");

    /**
     * 标签颜色
     */
    public final QueryColumn COLOR = new QueryColumn(this, "color");

    /**
     * 使用次数
     */
    public final QueryColumn USE_COUNT = new QueryColumn(this, "use_count");

    /**
     * 是否推荐标签 0-否 1-是
     */
    public final QueryColumn IS_RECOMMENDED = new QueryColumn(this, "is_recommended");

    /**
     * 状态：1-正常，0-禁用
     */
    public final QueryColumn STATUS = new QueryColumn(this, "status");

    /**
     * 创建时间
     */
    public final QueryColumn CREATED_AT = new QueryColumn(this, "created_at");

    /**
     * 更新时间
     */
    public final QueryColumn UPDATED_AT = new QueryColumn(this, "updated_at");

    /**
     * 逻辑删除：0-正常，1-已删除
     */
    public final QueryColumn IS_DELETED = new QueryColumn(this, "is_deleted");

    /**
     * 删除时间
     */
    public final QueryColumn DELETED_AT = new QueryColumn(this, "deleted_at");

    /**
     * 所有字段
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, NAME, TYPE, ICON, COLOR, USE_COUNT, IS_RECOMMENDED, STATUS};

    public LabelTableDef() {
        super("", "tb_label");
    }

    private LabelTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public LabelTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new LabelTableDef("", "tb_label", alias));
    }
}
