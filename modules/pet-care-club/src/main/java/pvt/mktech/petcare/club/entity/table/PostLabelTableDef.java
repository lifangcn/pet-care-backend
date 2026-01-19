package pvt.mktech.petcare.club.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

public class PostLabelTableDef extends TableDef {

    public static final PostLabelTableDef POST_LABEL = new PostLabelTableDef();
    public final QueryColumn ID = new QueryColumn(this, "id");
    public final QueryColumn POST_ID = new QueryColumn(this, "post_id");
    public final QueryColumn LABEL_ID = new QueryColumn(this, "label_id");
    public final QueryColumn CREATED_AT = new QueryColumn(this, "created_at");
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");
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
