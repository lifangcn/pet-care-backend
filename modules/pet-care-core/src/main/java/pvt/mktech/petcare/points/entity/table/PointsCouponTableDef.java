package pvt.mktech.petcare.points.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

/**
 * {@code @description}: 用户积分代金券表定义层
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
public class PointsCouponTableDef extends TableDef {

    /**
     * 用户积分代金券表
     */
    public static final PointsCouponTableDef POINTS_COUPON = new PointsCouponTableDef();

    /**
     * 主键ID
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 用户ID
     */
    public final QueryColumn USER_ID = new QueryColumn(this, "user_id");

    /**
     * 券模板ID
     */
    public final QueryColumn TEMPLATE_ID = new QueryColumn(this, "template_id");

    /**
     * 面值
     */
    public final QueryColumn FACE_VALUE = new QueryColumn(this, "face_value");

    /**
     * 状态
     */
    public final QueryColumn STATUS = new QueryColumn(this, "status");

    /**
     * 生效时间
     */
    public final QueryColumn START_TIME = new QueryColumn(this, "start_time");

    /**
     * 失效时间
     */
    public final QueryColumn END_TIME = new QueryColumn(this, "end_time");

    /**
     * 使用时间
     */
    public final QueryColumn USED_TIME = new QueryColumn(this, "used_time");

    /**
     * 使用时关联的流水ID
     */
    public final QueryColumn USED_RECORD_ID = new QueryColumn(this, "used_record_id");

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
            ID, USER_ID, TEMPLATE_ID, FACE_VALUE, STATUS, START_TIME,
            END_TIME, USED_TIME, USED_RECORD_ID, CREATED_AT
    };

    public PointsCouponTableDef() {
        super("", "tb_points_coupon");
    }

    private PointsCouponTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public PointsCouponTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new PointsCouponTableDef("", "tb_points_coupon", alias));
    }
}
