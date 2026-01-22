package pvt.mktech.petcare.social.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

public class ActivityTableDef extends TableDef {

    /**
     * 活动表
     */
    public static final ActivityTableDef ACTIVITY = new ActivityTableDef();

    /**
     * 主键ID
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 创建者ID
     */
    public final QueryColumn USER_ID = new QueryColumn(this, "user_id");

    /**
     * 活动标题
     */
    public final QueryColumn TITLE = new QueryColumn(this, "title");

    /**
     * 活动描述
     */
    public final QueryColumn DESCRIPTION = new QueryColumn(this, "description");

    /**
     * 封面图
     */
    public final QueryColumn COVER_IMAGE = new QueryColumn(this, "cover_image");

    /**
     * 1-线上活动 2-线下聚会
     */
    public final QueryColumn ACTIVITY_TYPE = new QueryColumn(this, "activity_type");

    /**
     * 活动时间
     */
    public final QueryColumn ACTIVITY_TIME = new QueryColumn(this, "activity_time");

    /**
     * 结束时间
     */
    public final QueryColumn END_TIME = new QueryColumn(this, "end_time");

    /**
     * 线下地址
     */
    public final QueryColumn ADDRESS = new QueryColumn(this, "address");

    /**
     * 线上链接
     */
    public final QueryColumn ONLINE_LINK = new QueryColumn(this, "online_link");

    /**
     * 最大参与人数 0-不限
     */
    public final QueryColumn MAX_PARTICIPANTS = new QueryColumn(this, "max_participants");

    /**
     * 当前参与人数
     */
    public final QueryColumn CURRENT_PARTICIPANTS = new QueryColumn(this, "current_participants");

    /**
     * 1-招募中 2-进行中 3-已结束
     */
    public final QueryColumn STATUS = new QueryColumn(this, "status");

    /**
     * 活动标签数组
     */
    public final QueryColumn LABELS = new QueryColumn(this, "labels");

    /**
     * 是否开启打卡 0-否 1-是
     */
    public final QueryColumn CHECK_IN_ENABLED = new QueryColumn(this, "check_in_enabled");

    /**
     * 打卡人数
     */
    public final QueryColumn CHECK_IN_COUNT = new QueryColumn(this, "check_in_count");

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
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, USER_ID, TITLE, DESCRIPTION, COVER_IMAGE, ACTIVITY_TYPE, ACTIVITY_TIME, END_TIME, ADDRESS, ONLINE_LINK, MAX_PARTICIPANTS, CURRENT_PARTICIPANTS, STATUS, LABELS, CHECK_IN_ENABLED, CHECK_IN_COUNT};

    public ActivityTableDef() {
        super("", "tb_activity");
    }

    private ActivityTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public ActivityTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new ActivityTableDef("", "tb_activity", alias));
    }
}
