package pvt.mktech.petcare.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;


/**
 * 用户表 表定义层。
 *
 * @author PetCare Code Generator
 * @since 2025-11-27
 */
public class UsersTableDef extends TableDef {

//    private static final long serialVersionUID = 9182519852047679441L;

    /**
     * 用户表
     */
    public static final UsersTableDef USERS = new UsersTableDef();

    /**
     * 用户ID
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 邮箱
     */
    public final QueryColumn EMAIL = new QueryColumn(this, "email");

    /**
     * 手机号
     */
    public final QueryColumn PHONE = new QueryColumn(this, "phone");

    /**
     * 性别: 0-未知 1-男 2-女
     */
    public final QueryColumn GENDER = new QueryColumn(this, "gender");

    /**
     * 状态: 0-禁用 1-正常 2-未激活
     */
    public final QueryColumn STATUS = new QueryColumn(this, "status");

    /**
     * 生日
     */
    public final QueryColumn BIRTHDAY = new QueryColumn(this, "birthday");

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
     * 最后登录时间
     */
    public final QueryColumn LAST_LOGIN_AT = new QueryColumn(this, "last_login_at");

    /**
     * 最后登录IP
     */
    public final QueryColumn LAST_LOGIN_IP = new QueryColumn(this, "last_login_ip");

    /**
     * 加密密码
     */
    public final QueryColumn PASSWORD_HASH = new QueryColumn(this, "password_hash");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, USERNAME, EMAIL, PHONE, PASSWORD_HASH, NICKNAME, AVATAR_URL, GENDER, BIRTHDAY, STATUS, LAST_LOGIN_AT, LAST_LOGIN_IP, CREATED_AT, UPDATED_AT};

    public UsersTableDef() {
        super("", "tb_user");
    }

    private UsersTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public UsersTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new UsersTableDef("", "tb_user", alias));
    }

}
