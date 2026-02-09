package pvt.mktech.petcare.points.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

/**
 * {@code @description}: 用户积分账户表定义层
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
public class PointsAccountTableDef extends TableDef {

    /**
     * 用户积分账户表
     */
    public static final PointsAccountTableDef POINTS_ACCOUNT = new PointsAccountTableDef();

    /**
     * 主键ID
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 用户ID
     */
    public final QueryColumn USER_ID = new QueryColumn(this, "user_id");

    /**
     * 当前可用积分
     */
    public final QueryColumn AVAILABLE_POINTS = new QueryColumn(this, "available_points");

    /**
     * 历史累计获取积分
     */
    public final QueryColumn TOTAL_POINTS = new QueryColumn(this, "total_points");

    /**
     * 创建时间
     */
    public final QueryColumn CREATED_AT = new QueryColumn(this, "created_at");

    /**
     * 更新时间
     */
    public final QueryColumn UPDATED_AT = new QueryColumn(this, "updated_at");

    /**
     * 所有字段
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{
            ID, USER_ID, AVAILABLE_POINTS, TOTAL_POINTS, CREATED_AT, UPDATED_AT
    };

    public PointsAccountTableDef() {
        super("", "tb_points_account");
    }

    private PointsAccountTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public PointsAccountTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new PointsAccountTableDef("", "tb_points_account", alias));
    }
}
