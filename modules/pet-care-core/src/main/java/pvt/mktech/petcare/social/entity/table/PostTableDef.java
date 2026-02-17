package pvt.mktech.petcare.social.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

public class PostTableDef extends TableDef {

    /**
     * 动态表
     */
    public static final PostTableDef POST = new PostTableDef();

    /**
     * 主键ID
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 发布者ID
     */
    public final QueryColumn USER_ID = new QueryColumn(this, "user_id");

    /**
     * 标题
     */
    public final QueryColumn TITLE = new QueryColumn(this, "title");

    /**
     * 内容描述
     */
    public final QueryColumn CONTENT = new QueryColumn(this, "content");

    /**
     * 1-好物分享 2-服务推荐 3-地点推荐 4-日常分享 5-活动打卡 6-活动报名
     */
    public final QueryColumn POST_TYPE = new QueryColumn(this, "post_type");

    /**
     * 图片/视频URL数组
     */
    public final QueryColumn MEDIA_URLS = new QueryColumn(this, "media_urls");

    /**
     * 外部链接（商品、服务、地图）
     */
    public final QueryColumn EXTERNAL_LINK = new QueryColumn(this, "external_link");

    /**
     * 地点信息
     */
    public final QueryColumn LOCATION_ADDRESS = new QueryColumn(this, "location_address");

    /**
     * 价格区间（如：100-200元）
     */
    public final QueryColumn PRICE_RANGE = new QueryColumn(this, "price_range");

    /**
     * 点赞数
     */
    public final QueryColumn LIKE_COUNT = new QueryColumn(this, "like_count");

    /**
     * 评分次数
     */
    public final QueryColumn RATING_COUNT = new QueryColumn(this, "rating_count");

    /**
     * 评分总分
     */
    public final QueryColumn RATING_TOTAL = new QueryColumn(this, "rating_total");

    /**
     * 平均分
     */
    public final QueryColumn RATING_AVG = new QueryColumn(this, "rating_avg");

    /**
     * 浏览量
     */
    public final QueryColumn VIEW_COUNT = new QueryColumn(this, "view_count");

    /**
     * 1-正常 2-隐藏
     */
    public final QueryColumn ENABLED = new QueryColumn(this, "enabled");

    /**
     * 关联的活动ID
     */
    public final QueryColumn ACTIVITY_ID = new QueryColumn(this, "activity_id");

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
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, USER_ID, TITLE, CONTENT, POST_TYPE, MEDIA_URLS, EXTERNAL_LINK, LOCATION_ADDRESS, PRICE_RANGE, LIKE_COUNT, RATING_COUNT, RATING_TOTAL, RATING_AVG, VIEW_COUNT, ENABLED, ACTIVITY_ID};

    public PostTableDef() {
        super("", "tb_post");
    }

    private PostTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public PostTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new PostTableDef("", "tb_post", alias));
    }
}
