package pvt.mktech.petcare.club.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

public class InteractionTableDef extends TableDef {

    public static final InteractionTableDef INTERACTION = new InteractionTableDef();
    public final QueryColumn ID = new QueryColumn(this, "id");
    public final QueryColumn USER_ID = new QueryColumn(this, "user_id");
    public final QueryColumn POST_ID = new QueryColumn(this, "post_id");
    public final QueryColumn INTERACTION_TYPE = new QueryColumn(this, "interaction_type");
    public final QueryColumn RATING_VALUE = new QueryColumn(this, "rating_value");
    public final QueryColumn CREATED_AT = new QueryColumn(this, "created_at");
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");
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
