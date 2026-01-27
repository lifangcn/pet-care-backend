package pvt.mktech.petcare.knowledgedocument.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

public class KnowledgeDocumentTableDef extends TableDef {

    /**
     * 知识文档表
     */
    public static final KnowledgeDocumentTableDef DOCUMENT = new KnowledgeDocumentTableDef();

    /**
     * 主键ID
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 文档名称
     */
    public final QueryColumn NAME = new QueryColumn(this, "name");

    /**
     * 状态：1-有效，0-禁用
     */
    public final QueryColumn STATUS = new QueryColumn(this, "status");

    /**
     * 文件Url
     */
    public final QueryColumn FILE_URL = new QueryColumn(this, "file_url");

    /**
     * 版本号
     */
    public final QueryColumn VERSION = new QueryColumn(this, "version");

    /**
     * 文件大小
     */
    public final QueryColumn FILE_SIZE = new QueryColumn(this, "file_size");

    /**
     * 文件类型
     */
    public final QueryColumn FILE_TYPE = new QueryColumn(this, "file_type");

    /**
     * 创建时间
     */
    public final QueryColumn CREATED_AT = new QueryColumn(this, "created_at");

    /**
     * 更新时间
     */
    public final QueryColumn UPDATED_AT = new QueryColumn(this, "updated_at");

    /**
     * 分块数量
     */
    public final QueryColumn CHUNK_COUNT = new QueryColumn(this, "chunk_count");

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
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, NAME, FILE_URL, FILE_TYPE, FILE_SIZE, VERSION, STATUS, CHUNK_COUNT};

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

