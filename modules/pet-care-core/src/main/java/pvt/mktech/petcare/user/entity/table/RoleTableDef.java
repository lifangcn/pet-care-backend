package pvt.mktech.petcare.user.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

/**
 * 角色表 表定义层。
 *
 * @author Michael Li
 * @since 2026-03-23
 */
public class RoleTableDef extends TableDef {

    /**
     * 角色表
     */
    public static final RoleTableDef ROLE = new RoleTableDef();

    /**
     * 角色ID
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 角色编码
     */
    public final QueryColumn ROLE_CODE = new QueryColumn(this, "role_code");

    /**
     * 角色名称
     */
    public final QueryColumn ROLE_NAME = new QueryColumn(this, "role_name");

    /**
     * 角色描述
     */
    public final QueryColumn DESCRIPTION = new QueryColumn(this, "description");

    /**
     * 排序
     */
    public final QueryColumn SORT = new QueryColumn(this, "sort");

    /**
     * 状态：0-禁用 1-启用
     */
    public final QueryColumn STATUS = new QueryColumn(this, "status");

    /**
     * 是否删除：0-未删除 1-已删除
     */
    public final QueryColumn IS_DELETED = new QueryColumn(this, "is_deleted");

    /**
     * 创建时间
     */
    public final QueryColumn CREATED_AT = new QueryColumn(this, "created_at");

    /**
     * 更新时间
     */
    public final QueryColumn UPDATED_AT = new QueryColumn(this, "updated_at");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    public RoleTableDef() {
        super("", "tb_role");
    }

    private RoleTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public RoleTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new RoleTableDef("", "tb_role", alias));
    }
}
