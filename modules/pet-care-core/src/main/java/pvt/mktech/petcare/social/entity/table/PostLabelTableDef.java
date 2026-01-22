package pvt.mktech.petcare.social.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

public class PostLabelTableDef extends TableDef {

    /**
     * 动态标签关联表
     */
    public static final PostLabelTableDef POST_LABEL = new PostLabelTableDef();

    /**
     * 主键ID
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 动态ID
     */
    public final QueryColumn POST_ID = new QueryColumn(this, "post_id");

    /**
     * 标签ID
     */
    public final QueryColumn LABEL_ID = new QueryColumn(this, "label_id");

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
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, POST_ID, LABEL_ID};

    public PostLabelTableDef() {
        super("", "tb_post_label");
    }

    private PostLabelTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public PostLabelTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new PostLabelTableDef("", "tb_post_label", alias));
    }
}
