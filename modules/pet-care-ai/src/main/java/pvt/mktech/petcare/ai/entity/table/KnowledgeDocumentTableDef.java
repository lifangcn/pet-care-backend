package pvt.mktech.petcare.ai.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

public class KnowledgeDocumentTableDef extends TableDef {

    public static final KnowledgeDocumentTableDef DOCUMENT = new KnowledgeDocumentTableDef();
    public final QueryColumn ID = new QueryColumn(this, "id");
    public final QueryColumn NAME = new QueryColumn(this, "name");
    public final QueryColumn STATUS = new QueryColumn(this, "status");
    public final QueryColumn FILE_URL = new QueryColumn(this, "file_url");
    public final QueryColumn VERSION = new QueryColumn(this, "version");
    public final QueryColumn FILE_SIZE = new QueryColumn(this, "file_size");
    public final QueryColumn FILE_TYPE = new QueryColumn(this, "file_type");
    public final QueryColumn CREATED_AT = new QueryColumn(this, "created_at");
    public final QueryColumn UPDATED_AT = new QueryColumn(this, "updated_at");
    public final QueryColumn CHUNK_COUNT = new QueryColumn(this, "chunk_count");
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, NAME, FILE_URL, FILE_TYPE, FILE_SIZE, VERSION, STATUS, CHUNK_COUNT, CREATED_AT, UPDATED_AT};

    public KnowledgeDocumentTableDef() {
        super("", "tb_knowledge_document");
    }

    private KnowledgeDocumentTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public KnowledgeDocumentTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new KnowledgeDocumentTableDef("", "tb_knowledge_document", alias));
    }
}

