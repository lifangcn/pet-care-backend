package pvt.mktech.petcare.user.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

/**
 * 用户角色关联表 表定义层。
 *
 * @author Michael Li
 * @since 2026-03-23
 */
public class UserRoleTableDef extends TableDef {

    /**
     * 用户角色关联表
     */
    public static final UserRoleTableDef USER_ROLE = new UserRoleTableDef();

    /**
     * 主键ID
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 用户ID
     */
    public final QueryColumn USER_ID = new QueryColumn(this, "user_id");

    /**
     * 角色ID
     */
    public final QueryColumn ROLE_ID = new QueryColumn(this, "role_id");

    /**
     * 创建时间
     */
    public final QueryColumn CREATED_AT = new QueryColumn(this, "created_at");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    public UserRoleTableDef() {
        super("", "tb_user_role");
    }

    private UserRoleTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public UserRoleTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new UserRoleTableDef("", "tb_user_role", alias));
    }
}
