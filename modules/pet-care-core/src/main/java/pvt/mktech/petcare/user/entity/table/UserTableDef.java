package pvt.mktech.petcare.user.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;


/**
 * 用户表 表定义层。
 *
 * @author PetCare Code Generator
 * @since 2025-11-27
 */
public class UserTableDef extends TableDef {

    /**
     * 用户表
     */
    public static final UserTableDef USER = new UserTableDef();

    /**
     * 用户ID
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 手机号
     */
    public final QueryColumn PHONE = new QueryColumn(this, "phone");


    /**
     * 状态: 0-禁用 1-正常 2-未激活
     */
    public final QueryColumn STATUS = new QueryColumn(this, "status");

    /**
     * 昵称
     */
    public final QueryColumn NICKNAME = new QueryColumn(this, "nickname");

    /**
     * 用户名
     */
    public final QueryColumn USERNAME = new QueryColumn(this, "username");

    /**
     * 头像URL
     */
    public final QueryColumn AVATAR = new QueryColumn(this, "avatar");

    /**
     * 生日
     */
    public final QueryColumn ADDRESS = new QueryColumn(this, "address");
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

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, USERNAME, PHONE, NICKNAME, AVATAR, STATUS, ADDRESS, CREATED_AT, UPDATED_AT};

    public UserTableDef() {
        super("", "tb_user");
    }

    private UserTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public UserTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new UserTableDef("", "tb_user", alias));
    }

}
