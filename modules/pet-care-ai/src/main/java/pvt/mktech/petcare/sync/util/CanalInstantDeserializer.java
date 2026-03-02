package pvt.mktech.petcare.sync.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * {@code @description}: Canal 日期时间反序列化器
 * <p>将 Canal 的 "yyyy-MM-dd HH:mm:ss" 格式字符串反序列化为 Instant</p>
 * {@code @date}: 2025-03-01
 * @author Michael Li
 */
public class CanalInstantDeserializer extends JsonDeserializer<Instant> {

    private static final DateTimeFormatter CANAL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String dateStr = p.getValueAsString();
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        LocalDateTime dateTime = LocalDateTime.parse(dateStr, CANAL_FORMAT);
        return dateTime.atZone(ZoneOffset.systemDefault()).toInstant();
    }
}
