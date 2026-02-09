package pvt.mktech.petcare.points.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

/**
 * {@code @description}: 积分流水记录表定义层
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
public class PointsRecordTableDef extends TableDef {

    /**
     * 积分流水记录表
     */
    public static final PointsRecordTableDef POINTS_RECORD = new PointsRecordTableDef();

    /**
     * 主键ID
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 用户ID
     */
    public final QueryColumn USER_ID = new QueryColumn(this, "user_id");

    /**
     * 积分变动值
     */
    public final QueryColumn POINTS = new QueryColumn(this, "points");

    /**
     * 变动前积分
     */
    public final QueryColumn POINTS_BEFORE = new QueryColumn(this, "points_before");

    /**
     * 变动后积分
     */
    public final QueryColumn POINTS_AFTER = new QueryColumn(this, "points_after");

    /**
     * 行为类型
     */
    public final QueryColumn ACTION_TYPE = new QueryColumn(this, "action_type");

    /**
     * 关联业务类型
     */
    public final QueryColumn BIZ_TYPE = new QueryColumn(this, "biz_type");

    /**
     * 关联业务ID
     */
    public final QueryColumn BIZ_ID = new QueryColumn(this, "biz_id");

    /**
     * 使用的代金券ID
     */
    public final QueryColumn COUPON_ID = new QueryColumn(this, "coupon_id");

    /**
     * 代金券抵扣积分数
     */
    public final QueryColumn COUPON_DEDUCT = new QueryColumn(this, "coupon_deduct");

    /**
     * 备注说明
     */
    public final QueryColumn REMARK = new QueryColumn(this, "remark");

    /**
     * 创建时间
     */
    public final QueryColumn CREATED_AT = new QueryColumn(this, "created_at");

    /**
     * 所有字段
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{
            ID, USER_ID, POINTS, POINTS_BEFORE, POINTS_AFTER, ACTION_TYPE,
            BIZ_TYPE, BIZ_ID, COUPON_ID, COUPON_DEDUCT, REMARK, CREATED_AT
    };

    public PointsRecordTableDef() {
        super("", "tb_points_record");
    }

    private PointsRecordTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public PointsRecordTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new PointsRecordTableDef("", "tb_points_record", alias));
    }
}
