package pvt.mktech.petcare.club.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

public class ActivityTableDef extends TableDef {

    public static final ActivityTableDef ACTIVITY = new ActivityTableDef();
    public final QueryColumn ID = new QueryColumn(this, "id");
    public final QueryColumn USER_ID = new QueryColumn(this, "user_id");
    public final QueryColumn TITLE = new QueryColumn(this, "title");
    public final QueryColumn DESCRIPTION = new QueryColumn(this, "description");
    public final QueryColumn COVER_IMAGE = new QueryColumn(this, "cover_image");
    public final QueryColumn ACTIVITY_TYPE = new QueryColumn(this, "activity_type");
    public final QueryColumn ACTIVITY_TIME = new QueryColumn(this, "activity_time");
    public final QueryColumn END_TIME = new QueryColumn(this, "end_time");
    public final QueryColumn ADDRESS = new QueryColumn(this, "address");
    public final QueryColumn ONLINE_LINK = new QueryColumn(this, "online_link");
    public final QueryColumn MAX_PARTICIPANTS = new QueryColumn(this, "max_participants");
    public final QueryColumn CURRENT_PARTICIPANTS = new QueryColumn(this, "current_participants");
    public final QueryColumn STATUS = new QueryColumn(this, "status");
    public final QueryColumn LABELS = new QueryColumn(this, "labels");
    public final QueryColumn CHECKIN_ENABLED = new QueryColumn(this, "checkin_enabled");
    public final QueryColumn CHECKIN_COUNT = new QueryColumn(this, "checkin_count");
    public final QueryColumn CREATED_AT = new QueryColumn(this, "created_at");
    public final QueryColumn UPDATED_AT = new QueryColumn(this, "updated_at");
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, USER_ID, TITLE, DESCRIPTION, COVER_IMAGE, ACTIVITY_TYPE, ACTIVITY_TIME, END_TIME, ADDRESS, ONLINE_LINK, MAX_PARTICIPANTS, CURRENT_PARTICIPANTS, STATUS, LABELS, CHECKIN_ENABLED, CHECKIN_COUNT};

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
