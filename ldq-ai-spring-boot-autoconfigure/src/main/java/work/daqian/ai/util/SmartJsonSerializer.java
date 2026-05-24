package work.daqian.ai.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class SmartJsonSerializer extends JsonSerializer<String> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        String trimmed = value.trim();
        
        if (trimmed.isEmpty()) {
            gen.writeString(value);
            return;
        }

        if (isJson(trimmed)) {
            gen.writeRawValue(value);  // JSON 字符串：直接输出，不添加引号
        } else {
            gen.writeString(value);     // 普通字符串：添加引号
        }
    }

    private boolean isJson(String str) {
        try {
            mapper.readTree(str);  // 尝试解析为 JSON
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}