package pvt.mktech.petcare.sync.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code @description}: JSON 字符串数组反序列化器
 * <p>将 MySQL JSON 字段在 Canal 中的字符串表示反序列化为 List</p>
 * {@code @date}: 2025-03-01
 * @author Michael Li
 */
public class JsonStringToListDeserializer extends JsonDeserializer<List<String>> {

    @Override
    public List<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String jsonStr = p.getValueAsString();
        if (jsonStr == null || jsonStr.isEmpty()) {
            return new ArrayList<>();
        }
        // Canal 对于 JSON 类型字段，返回的是 JSON 字符串
        // 使用 Jackson 的 ObjectMapper 再次解析
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        return mapper.readValue(jsonStr,
                mapper.getTypeFactory().constructCollectionType(List.class, String.class));
    }
}
