package pvt.mktech.petcare.sync.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * {@code @description}: Debezium CDC 事件消息
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Data
public class DebeziumEvent<T> {

    /**
     * 操作类型: c=create/r=read/u=update/d=delete
     */
    @JsonProperty("op")
    private String op;

    /**
     * 变更前的数据
     */
    @JsonProperty("before")
    private T before;

    /**
     * 变更后的数据
     */
    @JsonProperty("after")
    private T after;

    /**
     * 数据源元信息
     */
    @JsonProperty("source")
    private SourceInfo source;

    /**
     * 事务时间戳（毫秒）
     */
    @JsonProperty("ts_ms")
    private Long tsMs;

    /**
     * 数据源元信息
     */
    @Data
    public static class SourceInfo {
        @JsonProperty("version")
        private String version;

        @JsonProperty("connector")
        private String connector;

        @JsonProperty("name")
        private String name;

        @JsonProperty("ts_ms")
        private Long tsMs;

        @JsonProperty("snapshot")
        private String snapshot;

        @JsonProperty("db")
        private String db;

        @JsonProperty("sequence")
        private String sequence;

        @JsonProperty("table")
        private String table;

        @JsonProperty("server_id")
        private Long serverId;

        @JsonProperty("gtid")
        private String gtid;

        @JsonProperty("file")
        private String file;

        @JsonProperty("pos")
        private Long pos;

        @JsonProperty("row")
        private Integer row;

        @JsonProperty("thread")
        private Long thread;

        @JsonProperty("query")
        private String query;
    }
}
