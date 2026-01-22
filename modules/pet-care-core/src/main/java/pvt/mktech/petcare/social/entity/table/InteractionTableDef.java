package pvt.mktech.petcare.social.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

public class InteractionTableDef extends TableDef {

    /**
     * 互动表（点赞/评分）
     */
    public static final InteractionTableDef INTERACTION = new InteractionTableDef();

    /**
     * 主键ID
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 用户ID
     */
    public final QueryColumn USER_ID = new QueryColumn(this, "user_id");

    /**
     * 动态ID
     */
    public final QueryColumn POST_ID = new QueryColumn(this, "post_id");

    /**
     * 1-点赞 2-评分
     */
    public final QueryColumn INTERACTION_TYPE = new QueryColumn(this, "interaction_type");

    /**
     * 评分值 1-5
     */
    public final QueryColumn RATING_VALUE = new QueryColumn(this, "rating_value");

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
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, USER_ID, POST_ID, INTERACTION_TYPE, RATING_VALUE};

    public InteractionTableDef() {
        super("", "tb_interaction");
    }

    private InteractionTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public InteractionTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new InteractionTableDef("", "tb_interaction", alias));
    }
}
