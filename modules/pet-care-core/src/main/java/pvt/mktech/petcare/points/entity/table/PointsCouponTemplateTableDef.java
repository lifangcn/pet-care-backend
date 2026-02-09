package pvt.mktech.petcare.points.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

/**
 * {@code @description}: 积分代金券模板表定义层
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
public class PointsCouponTemplateTableDef extends TableDef {

    /**
     * 积分代金券模板表
     */
    public static final PointsCouponTemplateTableDef POINTS_COUPON_TEMPLATE = new PointsCouponTemplateTableDef();

    /**
     * 主键ID
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 券名称
     */
    public final QueryColumn NAME = new QueryColumn(this, "name");

    /**
     * 面值
     */
    public final QueryColumn FACE_VALUE = new QueryColumn(this, "face_value");

    /**
     * 有效天数
     */
    public final QueryColumn VALID_DAYS = new QueryColumn(this, "valid_days");

    /**
     * 发放总量
     */
    public final QueryColumn TOTAL_COUNT = new QueryColumn(this, "total_count");

    /**
     * 已发放数量
     */
    public final QueryColumn ISSUED_COUNT = new QueryColumn(this, "issued_count");

    /**
     * 每人限领数量
     */
    public final QueryColumn PER_USER_LIMIT = new QueryColumn(this, "per_user_limit");

    /**
     * 来源类型
     */
    public final QueryColumn SOURCE_TYPE = new QueryColumn(this, "source_type");

    /**
     * 状态
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
     * 所有字段
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{
            ID, NAME, FACE_VALUE, VALID_DAYS, TOTAL_COUNT, ISSUED_COUNT,
            PER_USER_LIMIT, SOURCE_TYPE, STATUS, CREATED_AT, UPDATED_AT
    };

    public PointsCouponTemplateTableDef() {
        super("", "tb_points_coupon_template");
    }

    private PointsCouponTemplateTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public PointsCouponTemplateTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new PointsCouponTemplateTableDef("", "tb_points_coupon_template", alias));
    }
}
