package pvt.mktech.petcare.club.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

public class PostTableDef extends TableDef {

    public static final PostTableDef POST = new PostTableDef();
    public final QueryColumn ID = new QueryColumn(this, "id");
    public final QueryColumn USER_ID = new QueryColumn(this, "user_id");
    public final QueryColumn TITLE = new QueryColumn(this, "title");
    public final QueryColumn CONTENT = new QueryColumn(this, "content");
    public final QueryColumn POST_TYPE = new QueryColumn(this, "post_type");
    public final QueryColumn MEDIA_URLS = new QueryColumn(this, "media_urls");
    public final QueryColumn EXTERNAL_LINK = new QueryColumn(this, "external_link");
    public final QueryColumn LOCATION_INFO = new QueryColumn(this, "location_info");
    public final QueryColumn PRICE_RANGE = new QueryColumn(this, "price_range");
    public final QueryColumn LIKE_COUNT = new QueryColumn(this, "like_count");
    public final QueryColumn RATING_COUNT = new QueryColumn(this, "rating_count");
    public final QueryColumn RATING_TOTAL = new QueryColumn(this, "rating_total");
    public final QueryColumn RATING_AVG = new QueryColumn(this, "rating_avg");
    public final QueryColumn VIEW_COUNT = new QueryColumn(this, "view_count");
    public final QueryColumn STATUS = new QueryColumn(this, "status");
    public final QueryColumn ACTIVITY_ID = new QueryColumn(this, "activity_id");
    public final QueryColumn CREATED_AT = new QueryColumn(this, "created_at");
    public final QueryColumn UPDATED_AT = new QueryColumn(this, "updated_at");
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, USER_ID, TITLE, CONTENT, POST_TYPE, MEDIA_URLS, EXTERNAL_LINK, LOCATION_INFO, PRICE_RANGE, LIKE_COUNT, RATING_COUNT, RATING_TOTAL, RATING_AVG, VIEW_COUNT, STATUS, ACTIVITY_ID};

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
