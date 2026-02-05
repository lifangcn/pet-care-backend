package pvt.mktech.petcare.sync.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * {@code @description}: Canal CDC 事件消息（Flat Message 格式）
 * <p>Canal 投递到 Kafka 的消息格式，data 是数组（批量变更）</p>
 * {@code @date}: 2026-02-05
 * @author Michael Li
 */
@Data
public class CanalEvent<T> {

    /**
     * 变更后的数据（数组，支持批量）
     */
    @JsonProperty("data")
    private List<T> data;

    /**
     * 变更前的数据（UPDATE 时有值）
     */
    @JsonProperty("old")
    private List<T> old;

    /**
     * 数据库名
     */
    @JsonProperty("database")
    private String database;

    /**
     * 表名
     */
    @JsonProperty("table")
    private String table;

    /**
     * 操作类型: INSERT/UPDATE/DELETE
     */
    @JsonProperty("type")
    private String type;

    /**
     * SQL 类型映射
     */
    @JsonProperty("sqlType")
    private Object sqlType;

    /**
     * MySQL 类型映射
     */
    @JsonProperty("mysqlType")
    private Object mysqlType;

    /**
     * 执行时间戳（秒）
     */
    @JsonProperty("es")
    private Long es;

    /**
     * GTID（全局事务ID）
     */
    @JsonProperty("gtid")
    private String gtid;

    /**
     * 事件ID
     */
    @JsonProperty("id")
    private Long id;

    /**
     * 是否为DDL语句
     */
    @JsonProperty("isDdl")
    private Boolean isDdl;

    /**
     * 主键字段名列表
     */
    @JsonProperty("pkNames")
    private List<String> pkNames;

    /**
     * SQL语句
     */
    @JsonProperty("sql")
    private String sql;

    /**
     * 时间戳（毫秒）
     */
    @JsonProperty("ts")
    private Long ts;
}
