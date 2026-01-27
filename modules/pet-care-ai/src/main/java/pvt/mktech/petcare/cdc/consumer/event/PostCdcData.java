package pvt.mktech.petcare.cdc.consumer.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * {@code @description}: Post CDC 数据（Debezium 消息体）
 * <p>仅包含 id，完整数据通过查询 core 数据库获取</p>
 * {@code @date}: 2026-01-27
 * @author Michael
 */
@Data
public class PostCdcData {

    @JsonProperty("id")
    private Long id;
}
