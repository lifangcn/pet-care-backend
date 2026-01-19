package pvt.mktech.petcare.club.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

public class LabelTableDef extends TableDef {

    public static final LabelTableDef LABEL = new LabelTableDef();
    public final QueryColumn ID = new QueryColumn(this, "id");
    public final QueryColumn NAME = new QueryColumn(this, "name");
    public final QueryColumn TYPE = new QueryColumn(this, "type");
    public final QueryColumn ICON = new QueryColumn(this, "icon");
    public final QueryColumn COLOR = new QueryColumn(this, "color");
    public final QueryColumn USE_COUNT = new QueryColumn(this, "use_count");
    public final QueryColumn IS_RECOMMENDED = new QueryColumn(this, "is_recommended");
    public final QueryColumn CREATED_AT = new QueryColumn(this, "created_at");
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, NAME, TYPE, ICON, COLOR, USE_COUNT, IS_RECOMMENDED};

    public LabelTableDef() {
        super("", "tb_label");
    }

    private LabelTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public LabelTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new LabelTableDef("", "tb_label", alias));
    }
}
